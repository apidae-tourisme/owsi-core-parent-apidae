package fr.openwide.core.etcd.cache.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import fr.openwide.core.etcd.common.exception.EtcdServiceException;
import fr.openwide.core.etcd.common.service.AbstractEtcdClientService;
import fr.openwide.core.etcd.common.utils.EtcdClientClusterConfiguration;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.kv.DeleteResponse;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.kv.PutResponse;

public abstract class AbstractEtcdCache<T extends IEtcdCacheValue> extends AbstractEtcdClientService implements IEtcdCache<T> {

	/**
	 * Use to Distinguish cache-related keys from other keys in the etcd store.
	 */
	private static final String CACHE_PREFIX = "cache/";
	private static final String CACHE_LIST_KEY = "cache/list";

	private final String cacheName;

	protected AbstractEtcdCache(String cacheName, EtcdClientClusterConfiguration config) {
		super(config);
		this.cacheName = cacheName;
	}

	protected T get(String key, Class<T> type) throws EtcdServiceException {
		try {
			String prefixedKey = getCacheKey(key);
			CompletableFuture<GetResponse> getFuture = getKvClient().get(ByteSequence.from(prefixedKey.getBytes()));
			GetResponse response = getFuture.get(10, TimeUnit.SECONDS);

			List<KeyValue> keyValues = response.getKvs();
			if (keyValues.isEmpty()) {
				return null;
			}
			byte[] serializedData = keyValues.get(0).getValue().getBytes();
			return deserializeObject(serializedData, type);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new EtcdServiceException("Failed to get value from cache '" + cacheName + "' for key: " + key, e);
		} catch (TimeoutException e) {
			throw new EtcdServiceException("Timeout while getting value from cache '" + cacheName + "' for key: " + key, e);
		} catch (Exception e) {
			throw new EtcdServiceException("Failed to get value from cache '" + cacheName + "' for key: " + key, e);
		}
	}

	@Override
	public void put(String key, T value) throws EtcdServiceException {
		try {
			String prefixedKey = getCacheKey(key);
			byte[] serializedData = serializeObject(value);

			CompletableFuture<PutResponse> putFuture = getKvClient().put(ByteSequence.from(prefixedKey.getBytes()),
					ByteSequence.from(serializedData));
			putFuture.get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new EtcdServiceException("Failed to put value in cache '" + cacheName + "' for key: " + key, e);
		} catch (Exception e) {
			throw new EtcdServiceException("Failed to put value in cache '" + cacheName + "' for key: " + key, e);
		}
	}

	protected T putIfAbsent(String key, T value, Class<T> type) throws EtcdServiceException {
		// First check if the key exists
		T existingValue = get(key, type);
		if (existingValue != null) {
			// Key exists, return the existing value
			return existingValue;
		}
		
		// Key doesn't exist, put the new value
		put(key, value);
		return null;
	}

	@Override
	public boolean delete(String key) throws EtcdServiceException {
		try {
			String prefixedKey = getCacheKey(key);
			CompletableFuture<DeleteResponse> deleteFuture = getKvClient()
					.delete(ByteSequence.from(prefixedKey.getBytes()));
			DeleteResponse response = deleteFuture.get();
			return response.getDeleted() > 0;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new EtcdServiceException("Failed to delete key from cache '" + cacheName + "': " + key, e);
		} catch (ExecutionException e) {
			throw new EtcdServiceException("Failed to delete key from cache '" + cacheName + "': " + key, e);
		}
	}

	@Override
	public List<String> getCacheNames() throws EtcdServiceException {
		try {
			CompletableFuture<GetResponse> getFuture = getKvClient().get(ByteSequence.from(CACHE_LIST_KEY.getBytes()));
			GetResponse response = getFuture.get();

			List<KeyValue> keyValues = response.getKvs();
			if (keyValues.isEmpty()) {
				return List.of();
			}

			String cacheList = new String(keyValues.get(0).getValue().getBytes(), StandardCharsets.UTF_8);
			return List.of(cacheList.split(","));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new EtcdServiceException("Failed to get cache names", e);
		} catch (ExecutionException e) {
			throw new EtcdServiceException("Failed to get cache names", e);
		}
	}

	@Override
	public void ensureCacheExists() throws EtcdServiceException {
		try {
			CompletableFuture<GetResponse> getFuture = getKvClient().get(ByteSequence.from(CACHE_LIST_KEY.getBytes()));
			GetResponse response = getFuture.get();

			List<KeyValue> keyValues = response.getKvs();
			if (keyValues.isEmpty()) {
				CompletableFuture<PutResponse> putFuture = getKvClient().put(
						ByteSequence.from(CACHE_LIST_KEY.getBytes()),
						ByteSequence.from(cacheName.getBytes(StandardCharsets.UTF_8)));
				putFuture.get();
				return;
			}
			String cacheList = new String(keyValues.get(0).getValue().getBytes(), StandardCharsets.UTF_8);
			if (!cacheList.contains(cacheName)) {
				String newCacheList = cacheList.isEmpty() ? cacheName : cacheList + "," + cacheName;
				CompletableFuture<PutResponse> putFuture = getKvClient().put(
						ByteSequence.from(CACHE_LIST_KEY.getBytes()),
						ByteSequence.from(newCacheList.getBytes(StandardCharsets.UTF_8)));
				putFuture.get();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new EtcdServiceException("Failed to ensure cache exists: " + cacheName, e);
		} catch (ExecutionException e) {
			throw new EtcdServiceException("Failed to ensure cache exists: " + cacheName, e);
		}
	}

	/**
	 * Build a key with the format "cache:<cacheName>:<key>"
	 */
	protected String getCacheKey(String key) {
		return getCachePrefix() + key;
	}

	public String getCachePrefix() {
		return CACHE_PREFIX + cacheName + ":";
	}

	/**
	 * Serializes a Serializable object to a byte array.
	 *
	 * @param obj The object to serialize
	 * @return The serialized object as a byte array
	 * @throws IOException If serialization fails
	 */
	protected byte[] serializeObject(T obj) throws IOException {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			oos.writeObject(obj);
			return baos.toByteArray();
		}
	}

	/**
	 * Deserializes a byte array back into an object of the specified type.
	 *
	 * @param data The serialized data
	 * @param type The expected type of the object
	 * @return The deserialized object
	 * @throws IOException            If deserialization fails
	 * @throws ClassNotFoundException If the class of the serialized object cannot
	 *                                be found
	 */
	protected T deserializeObject(byte[] data, Class<? extends T> type) throws IOException, ClassNotFoundException {
		try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
				ObjectInputStream ois = new ObjectInputStream(bais)) {
			Object obj = ois.readObject();
			if (!type.isInstance(obj)) {
				throw new ClassCastException("Deserialized object is not of type " + type.getName());
			}
			return type.cast(obj);
		}
	}

	@Override
	public List<String> getAllKeys() throws EtcdServiceException {
		try {
			String prefix = getCachePrefix();
			return getAllKeysFromPrefix(prefix);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new EtcdServiceException("Failed to get all keys from cache '" + cacheName + "'", e);
		} catch (Exception e) {
			throw new EtcdServiceException("Failed to get all keys from cache '" + cacheName + "'", e);
		}
	}

	protected Map<String, T> getAllValues(Class<T> type) throws EtcdServiceException {
		try {
			String prefix = getCachePrefix();
			List<KeyValue> keyValues = getAllFromPrefix(prefix);
			if (keyValues.isEmpty()) {
				return Map.of();
			}

			Map<String, T> kvMap = new HashMap<>();
			
			for(KeyValue kv : keyValues) {
				kvMap.put(new String(kv.getKey().getBytes(), StandardCharsets.UTF_8).substring(prefix.length()),
						deserializeObject(kv.getValue().getBytes(), type));
			}
			return kvMap;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new EtcdServiceException("Failed to get all values from cache '" + cacheName + "'", e);
		} catch (Exception e) {
			throw new EtcdServiceException("Failed to get all values from cache '" + cacheName + "'", e);
		}
	}

	/**
	 * Deletes all keys in the cache in a single atomic operation. This method uses
	 * etcd's range delete functionality to efficiently remove all keys that match
	 * the cache's prefix.
	 *
	 * @throws EtcdServiceException if the deletion operation fails or if the
	 *                              operation is interrupted
	 */
	@Override
	public void deleteAllCacheKeys() throws EtcdServiceException {
		try {
			deleteAllKeysFromPrefix(getCachePrefix());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new EtcdServiceException("Failed to delete all keys from cache '" + cacheName + "'", e);
		} catch (Exception e) {
			throw new EtcdServiceException("Failed to delete all keys from cache '" + cacheName + "'", e);
		}
	}

	/**
	 * Deletes the current cache and all its associated keys from etcd. This method
	 * removes the cache from the cache list and deletes all keys associated with
	 * this specific cache.
	 *
	 * @throws EtcdServiceException if the deletion operation fails or if the
	 *                              operation is interrupted
	 */
	public void deleteCache() throws EtcdServiceException {
		try {
			// First delete all keys with this cache's prefix
			deleteAllCacheKeys();

			// Then remove this cache from the cache list
			List<String> cacheNames = getCacheNames();
			if (cacheNames.contains(cacheName)) {
				List<String> remainingCaches = cacheNames.stream().filter(name -> !name.equals(cacheName)).toList();

				String newCacheList = String.join(",", remainingCaches);
				CompletableFuture<PutResponse> putFuture = getKvClient().put(
						ByteSequence.from(CACHE_LIST_KEY.getBytes()),
						ByteSequence.from(newCacheList.getBytes(StandardCharsets.UTF_8)));
				putFuture.get();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new EtcdServiceException("Failed to delete cache '" + cacheName + "'", e);
		} catch (Exception e) {
			throw new EtcdServiceException("Failed to delete cache '" + cacheName + "'", e);
		}
	}

	public String getCacheName() {
		return cacheName;
	}

}
