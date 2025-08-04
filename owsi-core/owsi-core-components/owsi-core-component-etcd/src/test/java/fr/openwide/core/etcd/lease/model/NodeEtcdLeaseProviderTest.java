package fr.openwide.core.etcd.lease.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import fr.openwide.core.etcd.common.exception.EtcdServiceException;
import fr.openwide.core.etcd.common.utils.EtcdCommonClusterConfiguration;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.lease.LeaseGrantResponse;
import io.etcd.jetcd.lease.LeaseRevokeResponse;

public class NodeEtcdLeaseProviderTest {

	@Mock
	private Client mockClient;
	
	@Mock
	private Lease mockLeaseClient;
	
	@Mock
	private LeaseGrantResponse mockLeaseGrantResponse;
	
	private EtcdCommonClusterConfiguration config;
	private AtomicBoolean isShutdown;
	private NodeEtcdLeaseProvider leaseProvider;
	
	private static final String NODE_NAME = "test-node";
	private static final long LEASE_TTL = 30L;
	private static final long TEST_LEASE_ID = 123456L;

	@Before
	public void setUp() {
		MockitoAnnotations.openMocks(this);
		
		config = EtcdCommonClusterConfiguration.builder()
			.withNodeName(NODE_NAME)
			.withEndpoints("http://localhost:2379")
			.withClusterName("test-cluster")
			.withLeaseTtl(LEASE_TTL)
			.build();
		
		isShutdown = new AtomicBoolean(false);
		leaseProvider = new NodeEtcdLeaseProvider(mockClient, config, isShutdown);
		
		// Configuration de base des mocks
		when(mockClient.getLeaseClient()).thenReturn(mockLeaseClient);
		when(mockLeaseGrantResponse.getID()).thenReturn(TEST_LEASE_ID);
	}

	@Test
	public void testCreateNewLeaseWithKeepAlive() throws Exception {
		// Given
		CompletableFuture<LeaseGrantResponse> leaseGrantFuture = CompletableFuture.completedFuture(mockLeaseGrantResponse);
		when(mockLeaseClient.grant(LEASE_TTL)).thenReturn(leaseGrantFuture);
		
		// When
		Long leaseId = leaseProvider.getNewLeaseIdWithKeepAlive();
		
		// Then
		assertThat(leaseId).isEqualTo(TEST_LEASE_ID);
		verify(mockLeaseClient).grant(LEASE_TTL);
		verify(mockLeaseClient).keepAlive(eq(TEST_LEASE_ID), any());
	}

	@Test
	public void testGlobalLeaseCreationSynchronized() throws Exception {
		// Given
		CompletableFuture<LeaseGrantResponse> leaseGrantFuture = CompletableFuture.completedFuture(mockLeaseGrantResponse);
		when(mockLeaseClient.grant(LEASE_TTL)).thenReturn(leaseGrantFuture);
		
		AtomicInteger leaseCreationCount = new AtomicInteger(0);
		doAnswer(invocation -> {
			leaseCreationCount.incrementAndGet();
			return leaseGrantFuture;
		}).when(mockLeaseClient).grant(LEASE_TTL);
		
		// When - appels multiples simultanés
		Long leaseId1 = leaseProvider.getGlobalClientLeaseIdWithKeepAlive();
		Long leaseId2 = leaseProvider.getGlobalClientLeaseIdWithKeepAlive();
		Long leaseId3 = leaseProvider.getGlobalClientLeaseIdWithKeepAlive();
		
		// Then - le même lease est retourné et n'est créé qu'une seule fois
		assertThat(leaseId1).isEqualTo(TEST_LEASE_ID);
		assertThat(leaseId2).isEqualTo(TEST_LEASE_ID);
		assertThat(leaseId3).isEqualTo(TEST_LEASE_ID);
		assertThat(leaseCreationCount.get()).isEqualTo(1);
	}

	@Test
	public void testLeaseRecoveryAfterRevocationWithDifferentId() throws Exception {
		// Given - Configuration des mocks pour retourner des IDs de lease différents
		long firstLeaseId = TEST_LEASE_ID;
		long secondLeaseId = TEST_LEASE_ID + 2000; // Nouveau lease avec un ID différent
		
		LeaseGrantResponse mockFirstLeaseResponse = mock(LeaseGrantResponse.class);
		when(mockFirstLeaseResponse.getID()).thenReturn(firstLeaseId);
		
		LeaseGrantResponse mockSecondLeaseResponse = mock(LeaseGrantResponse.class);
		when(mockSecondLeaseResponse.getID()).thenReturn(secondLeaseId);
		
		CompletableFuture<LeaseGrantResponse> firstLeaseGrantFuture = CompletableFuture.completedFuture(mockFirstLeaseResponse);
		CompletableFuture<LeaseGrantResponse> secondLeaseGrantFuture = CompletableFuture.completedFuture(mockSecondLeaseResponse);
		
		// Configurer les mocks pour retourner des leases différents à chaque appel
		when(mockLeaseClient.grant(LEASE_TTL))
			.thenReturn(firstLeaseGrantFuture)
			.thenReturn(secondLeaseGrantFuture);
		
		// When - premier appel pour créer le lease
		Long actualFirstLeaseId = leaseProvider.getGlobalClientLeaseIdWithKeepAlive();
		assertThat(actualFirstLeaseId).isEqualTo(firstLeaseId);
		
		// Simuler l'invalidation du lease en utilisant revokeLease (plus réaliste qu'une erreur "lease not found")
		// Configuration du mock pour la révocation
		CompletableFuture<io.etcd.jetcd.lease.LeaseRevokeResponse> revokeResponseFuture = 
			CompletableFuture.completedFuture(mock(io.etcd.jetcd.lease.LeaseRevokeResponse.class));
		when(mockLeaseClient.revoke(firstLeaseId)).thenReturn(revokeResponseFuture);
		
		// Révoquer le lease actuel pour déclencher la création d'un nouveau
		leaseProvider.revokeLease(actualFirstLeaseId);
		
		// When - deuxième appel après révocation du lease
		Long actualSecondLeaseId = leaseProvider.getGlobalClientLeaseIdWithKeepAlive();
		
		// Then - un nouveau lease avec un ID différent doit être créé
		assertThat(actualSecondLeaseId).isEqualTo(secondLeaseId).isNotEqualTo(actualFirstLeaseId);
		verify(mockLeaseClient, times(2)).grant(LEASE_TTL);
		verify(mockLeaseClient).revoke(firstLeaseId); // Vérifier que la révocation a été appelée
		verify(mockLeaseClient).keepAlive(eq(firstLeaseId), any());
		verify(mockLeaseClient).keepAlive(eq(secondLeaseId), any());
	}

	@Test
	public void testLeaseRecoveryAfterRevocation() throws Exception {
		// Given
		CompletableFuture<LeaseGrantResponse> leaseGrantFuture = CompletableFuture.completedFuture(mockLeaseGrantResponse);
		when(mockLeaseClient.grant(LEASE_TTL)).thenReturn(leaseGrantFuture);
		
		// When - créer le lease initial
		Long firstLeaseId = leaseProvider.getGlobalClientLeaseIdWithKeepAlive();
		assertThat(firstLeaseId).isEqualTo(TEST_LEASE_ID);
		
		// Simuler l'invalidation du lease après trop d'erreurs consécutives en utilisant revokeLease
		// Configuration du mock pour la révocation
		CompletableFuture<LeaseRevokeResponse> revokeResponseFuture = 
			CompletableFuture.completedFuture(mock(LeaseRevokeResponse.class));
		when(mockLeaseClient.revoke(firstLeaseId)).thenReturn(revokeResponseFuture);
		
		// Révoquer le lease pour simuler l'invalidation après MAX_CONSECUTIVE_ERRORS
		leaseProvider.revokeLease(firstLeaseId);
		
		// When - deuxième appel après révocation du lease
		Long secondLeaseId = leaseProvider.getGlobalClientLeaseIdWithKeepAlive();
		
		// Then - un nouveau lease doit être créé après révocation
		assertThat(secondLeaseId).isEqualTo(TEST_LEASE_ID);
		verify(mockLeaseClient, times(2)).grant(LEASE_TTL);
		verify(mockLeaseClient).revoke(firstLeaseId); // Vérifier que la révocation a été appelée
		verify(mockLeaseClient, times(2)).keepAlive(eq(TEST_LEASE_ID), any());
	}

	@Test
	public void testLeaseExpirationDetection() throws Exception {
		// Given - Configuration avec un TTL très court pour simuler l'expiration
		EtcdCommonClusterConfiguration shortTtlConfig = EtcdCommonClusterConfiguration.builder()
			.withNodeName(NODE_NAME)
			.withLeaseTtl(1L) // 1 seconde pour les tests
			.withEndpoints("http://localhost:2379")
			.withClusterName("test-cluster")
			.build();
		
		NodeEtcdLeaseProvider shortTtlProvider = new NodeEtcdLeaseProvider(mockClient, shortTtlConfig, isShutdown);
		
		// Configuration des mocks pour supporter plusieurs appels avec des IDs différents
		long firstLeaseId = TEST_LEASE_ID;
		long secondLeaseId = TEST_LEASE_ID + 1000; // Nouveau lease avec un ID différent
		
		LeaseGrantResponse mockFirstLeaseResponse = mock(LeaseGrantResponse.class);
		when(mockFirstLeaseResponse.getID()).thenReturn(firstLeaseId);
		
		LeaseGrantResponse mockSecondLeaseResponse = mock(LeaseGrantResponse.class);
		when(mockSecondLeaseResponse.getID()).thenReturn(secondLeaseId);
		
		CompletableFuture<LeaseGrantResponse> firstLeaseGrantFuture = CompletableFuture.completedFuture(mockFirstLeaseResponse);
		CompletableFuture<LeaseGrantResponse> secondLeaseGrantFuture = CompletableFuture.completedFuture(mockSecondLeaseResponse);
		
		// Configurer les mocks pour retourner des leases différents à chaque appel
		when(mockLeaseClient.grant(1L))
			.thenReturn(firstLeaseGrantFuture)
			.thenReturn(secondLeaseGrantFuture);
		
		// When - créer le lease initial
		Long actualFirstLeaseId = shortTtlProvider.getGlobalClientLeaseIdWithKeepAlive();
		assertThat(actualFirstLeaseId).isEqualTo(firstLeaseId);
		
		CompletableFuture<Long> secondLeaseIdFuture = CompletableFuture.supplyAsync(() -> {
			try {
				return shortTtlProvider.getGlobalClientLeaseIdWithKeepAlive();
			} catch (Exception e) {
				throw new IllegalStateException("Erreur lors de la récupération du lease après expiration", e);
			}
		}, CompletableFuture.delayedExecutor(900, TimeUnit.MILLISECONDS)); // Attendre l'expiration
		
		// When - demander le lease après expiration (Attendre l'expiration à 80%  de 1 seconde = 0.8 seconde)
		Long actualSecondLeaseId = secondLeaseIdFuture.get(2, TimeUnit.SECONDS);
		
		// Then - Un nouveau lease avec un ID différent doit être créé
		assertThat(actualSecondLeaseId).isEqualTo(secondLeaseId).isNotEqualTo(actualFirstLeaseId);
		verify(mockLeaseClient, times(2)).grant(1L);
	}

	@Test
	public void testLeaseCreationFailure() throws Exception {
		// Given
		CompletableFuture<LeaseGrantResponse> failedFuture = new CompletableFuture<>();
		failedFuture.completeExceptionally(new ExecutionException("Lease creation failed", new RuntimeException()));
		when(mockLeaseClient.grant(LEASE_TTL)).thenReturn(failedFuture);
		
		// When & Then
		assertThatThrownBy(() -> leaseProvider.getGlobalClientLeaseIdWithKeepAlive())
			.isInstanceOf(EtcdServiceException.class)
			.hasMessageContaining("ExecutionException while trying to create lease");
	}

	@Test
	public void testShutdownBehavior() throws Exception {
		// Given
		isShutdown.set(true);
		CompletableFuture<LeaseGrantResponse> leaseGrantFuture = CompletableFuture.completedFuture(mockLeaseGrantResponse);
		when(mockLeaseClient.grant(LEASE_TTL)).thenReturn(leaseGrantFuture);
		
		// When
		Long leaseId = leaseProvider.getNewLeaseIdWithKeepAlive();
		
		// Then - le lease est créé mais keep-alive ne doit pas être démarré
		assertThat(leaseId).isEqualTo(TEST_LEASE_ID);
		verify(mockLeaseClient).grant(LEASE_TTL);
		// Le keep-alive ne doit pas être appelé car isShutdown = true
		verify(mockLeaseClient, times(0)).keepAlive(eq(TEST_LEASE_ID), any());
	}

	@Test
	public void testMaxConsecutiveErrorsConstant() throws Exception {
		// Test simple pour vérifier que MAX_CONSECUTIVE_ERRORS est configuré à 3
		// En utilisant la réflexion pour lire la constante
		java.lang.reflect.Field maxErrorsField = NodeEtcdLeaseProvider.class.getDeclaredField("MAX_CONSECUTIVE_ERRORS");
		maxErrorsField.setAccessible(true);
		int maxConsecutiveErrors = (Integer) maxErrorsField.get(null); // field statique
		
		// Then - Vérifier que le seuil est bien configuré à 3 (réduit de 4)
		assertThat(maxConsecutiveErrors).as("MAX_CONSECUTIVE_ERRORS doit être configuré à 3").isEqualTo(3);
	}
} 