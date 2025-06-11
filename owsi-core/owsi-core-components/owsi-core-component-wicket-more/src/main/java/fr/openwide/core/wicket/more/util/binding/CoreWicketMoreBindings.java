package fr.openwide.core.wicket.more.util.binding;

import org.bindgen.java.util.ListBinding;

import fr.openwide.core.commons.util.mime.MediaTypeBinding;
import fr.openwide.core.etcd.cache.model.lockattribution.LockAttributionEtcdValueBinding;
import fr.openwide.core.etcd.cache.model.node.NodeEtcdValueBinding;
import fr.openwide.core.etcd.cache.model.priorityqueue.PriorityQueueEtcdValueBinding;
import fr.openwide.core.etcd.cache.model.queuedtask.QueuedTaskEtcdValueBinding;
import fr.openwide.core.etcd.cache.model.role.RoleEtcdValueBinding;
import fr.openwide.core.etcd.cache.model.rolerequest.RoleRequestEtcdValueBinding;
import fr.openwide.core.etcd.lock.model.EtcdLockBinding;
import fr.openwide.core.infinispan.model.IAttributionBinding;
import fr.openwide.core.infinispan.model.ILockAttributionBinding;
import fr.openwide.core.infinispan.model.ILockBinding;
import fr.openwide.core.infinispan.model.INodeBinding;
import fr.openwide.core.infinispan.model.IRoleAttributionBinding;
import fr.openwide.core.infinispan.model.IRoleBinding;
import fr.openwide.core.jpa.more.infinispan.model.TaskQueueStatusBinding;
import fr.openwide.core.wicket.more.console.maintenance.ehcache.model.EhCacheCacheInformationBinding;
import fr.openwide.core.wicket.more.model.IBindableDataProviderBinding;

public final class CoreWicketMoreBindings {

	private static final EhCacheCacheInformationBinding EH_CACHE_CACHE_INFORMATION = new EhCacheCacheInformationBinding();

	private static final IBindableDataProviderBinding IBINDABLE_DATA_PROVIDER = new IBindableDataProviderBinding();

	private static final ListBinding<?> LIST = new ListBinding<Void>();

	private static final MediaTypeBinding MEDIA_TYPE = new MediaTypeBinding();

	private static final INodeBinding I_NODE = new INodeBinding();

	private static final NodeEtcdValueBinding NODE_ETCD_VALUE = new NodeEtcdValueBinding();
	private static final RoleEtcdValueBinding ROLE_ETCD_VALUE = new RoleEtcdValueBinding();
	private static final LockAttributionEtcdValueBinding LOCK_ATTRIBUTION_ETCD_VALUE = new LockAttributionEtcdValueBinding();
	private static final PriorityQueueEtcdValueBinding PRIORITY_QUEUE_ETCD_VALUE = new PriorityQueueEtcdValueBinding();

	private static final ILockBinding I_LOCK = new ILockBinding();
	private static final ILockAttributionBinding I_LOCK_ATTRIBUTION = new ILockAttributionBinding();

	private static final EtcdLockBinding ETCD_LOCK = new EtcdLockBinding();

	private static final QueuedTaskEtcdValueBinding QUEUED_TASK_ETCD_VALUE = new QueuedTaskEtcdValueBinding();

	private static final IRoleBinding I_ROLE = new IRoleBinding();
	private static final IRoleAttributionBinding I_ROLE_ATTRIBUTION = new IRoleAttributionBinding();

	private static final IAttributionBinding I_ATTRIBUTION = new IAttributionBinding();

	private static final TaskQueueStatusBinding TASK_QUEUE_STATUS = new TaskQueueStatusBinding(); 

	private static final RoleRequestEtcdValueBinding ROLE_REQUEST_ETCD_VALUE = new RoleRequestEtcdValueBinding();

	public static EhCacheCacheInformationBinding ehCacheCacheInformation() {
		return EH_CACHE_CACHE_INFORMATION;
	}

	public static IBindableDataProviderBinding iBindableDataProvider() {
		return IBINDABLE_DATA_PROVIDER;
	}

	public static NodeEtcdValueBinding nodeEtcdValue() {
		return NODE_ETCD_VALUE;
	}

	public static RoleEtcdValueBinding rodeEtcdValue() {
		return ROLE_ETCD_VALUE;
	}

	public static RoleRequestEtcdValueBinding rodeRequestEtcdValue() {
		return ROLE_REQUEST_ETCD_VALUE;
	}

	public static EtcdLockBinding etcdLock() {
		return ETCD_LOCK;
	}

	public static QueuedTaskEtcdValueBinding queuedTaskEtcdValue() {
		return QUEUED_TASK_ETCD_VALUE;
	}

	public static ListBinding<?> list() {
		return LIST;
	}

	public static MediaTypeBinding mediaType() {
		return MEDIA_TYPE;
	}

	public static INodeBinding iNode() {
		return I_NODE;
	}

	public static ILockBinding iLock() {
		return I_LOCK;
	}

	public static ILockAttributionBinding iLockAttribution() {
		return I_LOCK_ATTRIBUTION;
	}

	public static LockAttributionEtcdValueBinding lockAttributionEtcd() {
		return LOCK_ATTRIBUTION_ETCD_VALUE;
	}

	public static PriorityQueueEtcdValueBinding priorityQueueEtcdValue() {
		return PRIORITY_QUEUE_ETCD_VALUE;
	}

	public static IRoleBinding iRole() {
		return I_ROLE;
	}

	public static IRoleAttributionBinding iRoleAttribution() {
		return I_ROLE_ATTRIBUTION;
	}

	public static IAttributionBinding iAttribution(){
		return I_ATTRIBUTION;
	}

	public static TaskQueueStatusBinding taskQueueStatus(){
		return TASK_QUEUE_STATUS;
	}

	private CoreWicketMoreBindings() {
	}

}
