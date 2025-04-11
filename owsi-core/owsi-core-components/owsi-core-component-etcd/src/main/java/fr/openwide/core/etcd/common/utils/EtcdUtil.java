package fr.openwide.core.etcd.common.utils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import io.etcd.jetcd.ByteSequence;

public class EtcdUtil {

	private static final byte[] NO_PREFIX_END = { 0 };

	/**
	 * Computes the end key for a prefix to be used with etcd range queries. This
	 * gives the lexicographical upper bound for a prefix scan.
	 */
	public static ByteSequence prefixEndOf(String prefix) {
		byte[] endKey = prefix.getBytes(StandardCharsets.UTF_8);
		for (int i = endKey.length - 1; i >= 0; i--) {
			if (endKey[i] != (byte) 0xff) {
				endKey[i] = (byte) (endKey[i] + 1);
				return ByteSequence.from(Arrays.copyOf(endKey, i + 1));
			}
		}
		return ByteSequence.from(NO_PREFIX_END);
	}

}
