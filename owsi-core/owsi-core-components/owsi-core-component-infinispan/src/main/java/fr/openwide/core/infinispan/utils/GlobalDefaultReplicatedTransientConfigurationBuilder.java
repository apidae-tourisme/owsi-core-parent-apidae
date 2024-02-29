package fr.openwide.core.infinispan.utils;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Properties;
import java.util.Set;

import org.infinispan.commons.marshall.AdvancedExternalizer;
import org.infinispan.commons.marshall.InstanceReusingAdvancedExternalizer;
import org.infinispan.commons.util.Util;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationChildBuilder;
import org.infinispan.configuration.global.GlobalJmxStatisticsConfigurationBuilder;
import org.infinispan.configuration.global.TransportConfigurationBuilder;
import org.infinispan.jboss.marshalling.core.JBossUserMarshaller;
import org.infinispan.jmx.PlatformMBeanServerLookup;
import org.infinispan.remoting.transport.jgroups.JGroupsAddress;
import org.infinispan.remoting.transport.jgroups.JGroupsAddressCache;

public class GlobalDefaultReplicatedTransientConfigurationBuilder extends GlobalConfigurationBuilder {

	public GlobalDefaultReplicatedTransientConfigurationBuilder() {
		this(null);
	}

	public GlobalDefaultReplicatedTransientConfigurationBuilder(Properties transportProperties) {
		super();
		// not planned to use JBoss, so we use directly PlatformMBeanServerLookup
		globalJmxStatistics().mBeanServerLookup(new PlatformMBeanServerLookup());
		Properties properties = new Properties();
		if (transportProperties != null) {
			properties.putAll(transportProperties);
		}
		//String jgroupsConfigurationFile = "default-configs/enhanced-jgroups-udp.xml";
		String jgroupsConfigurationFile = "default-configs/enhanced-jgroups-ec2.xml";
		if (properties.containsKey("configurationFile")) {
			jgroupsConfigurationFile = transportProperties.getProperty("configurationFile");
			transportProperties.remove("configurationFile");
		}
		this.defaultCacheName("defaultCache");
		serialization().marshaller(new JBossUserMarshaller());
		serialization().addAdvancedExternalizer((AdvancedExternalizer<?>) new Externalizer());
		transport().defaultTransport().addProperty("configurationFile", jgroupsConfigurationFile);
		// Jgroups needs to lookup placeholder values in system properties
		System.getProperties().putAll(properties);
	}

	/**
	 * @see GlobalJmxStatisticsConfigurationBuilder#cacheManagerName(String)
	 */
	public GlobalConfigurationBuilder cacheManagerName(String cacheManagerName) {
		globalJmxStatistics().enable().jmxDomain(cacheManagerName);
		return this.cacheManagerName(cacheManagerName);
	}

	/**
	 * @see TransportConfigurationBuilder#nodeName(String)
	 */
	public GlobalConfigurationChildBuilder nodeName(String nodeName) {
		return transport().nodeName(nodeName);
	}

	/**
	 * Extracted from {@link JGroupsAddress}.
	 */
	public static final class Externalizer extends InstanceReusingAdvancedExternalizer<JGroupsAddress> {

		public Externalizer() {
			super(false);
		}

		@Override
		public void doWriteObject(ObjectOutput output, JGroupsAddress address) throws IOException {
			try {
				org.jgroups.util.Util.writeAddress(address.getJGroupsAddress(), output);
			} catch (Exception e) {
				throw new IOException(e);
			}
		}

		@Override
		public JGroupsAddress doReadObject(ObjectInput unmarshaller) throws IOException, ClassNotFoundException {
			try {
				// Note: Use org.jgroups.Address, not the concrete UUID class.
				// Otherwise applications that only use local caches would have to bundle the
				// JGroups jar,
				// because the verifier needs to check the arguments of fromJGroupsAddress
				// even if this method is never called.
				org.jgroups.Address address = org.jgroups.util.Util.readAddress(unmarshaller);
				return (JGroupsAddress) JGroupsAddressCache.fromJGroupsAddress(address);
			} catch (Exception e) {
				throw new IOException(e);
			}
		}

		@Override
		public Integer getId() {
			return JBossUserMarshaller.USER_EXT_ID_MIN + 1;
		}

		@Override
		public Set<Class<? extends JGroupsAddress>> getTypeClasses() {
			return Util.<Class<? extends JGroupsAddress>>asSet(JGroupsAddress.class);
		}
	}

}
