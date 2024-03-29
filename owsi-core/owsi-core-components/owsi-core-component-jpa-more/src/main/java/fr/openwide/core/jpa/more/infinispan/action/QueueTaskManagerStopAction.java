package fr.openwide.core.jpa.more.infinispan.action;

import org.infinispan.remoting.transport.Address;
import org.infinispan.remoting.transport.jgroups.JGroupsAddress;
import org.springframework.beans.factory.annotation.Autowired;

import fr.openwide.core.infinispan.model.impl.SimpleAction;
import fr.openwide.core.jpa.more.infinispan.service.IInfinispanQueueTaskManagerService;

public class QueueTaskManagerStopAction extends SimpleAction<SwitchStatusQueueTaskManagerResult> {

	private static final long serialVersionUID = -5968225724015355537L;

	@Autowired
	private transient IInfinispanQueueTaskManagerService infinispanQueueTaskManagerService;

	protected QueueTaskManagerStopAction(Address target) {
		super(target, false, true);
	}

	@Override
	public SwitchStatusQueueTaskManagerResult call() throws Exception {
		return infinispanQueueTaskManagerService.stop();
	}

	public static final QueueTaskManagerStopAction get(Address address){
		return new QueueTaskManagerStopAction(address);
	}

}
