package fr.openwide.core.etcd.cache.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.openwide.core.etcd.common.exception.EtcdServiceException;

public interface IEtcdCache<T extends Serializable> {
	

	/**
	 * Gets a value from a specific cache for the given key.
	 * 
	 * @param key The key to retrieve
	 * @return The value associated with the key, or null if not found
	 * @throws Exception If an error occurs during the operation
	 */
	T get(String key) throws EtcdServiceException;
	
	T remove(String key) throws EtcdServiceException;

	/**
	 * Puts a value into a specific cache for the given key.
	 * 
	 * @param key The key to store
	 * @param value The value to store (must be Serializable)
	 * @throws Exception If an error occurs during the operation
	 */
	void put(String key, T value) throws EtcdServiceException;
	
	T putIfAbsent(String key, T value) throws EtcdServiceException;

	/**
	 * Deletes a key from a specific cache.
	 * 
	 * @param key The key to delete
	 * @return true if the key was found and deleted, false if the key didn't exist
	 * @throws EtcdServiceException If an error occurs during the operation
	 */
	boolean delete(String key) throws EtcdServiceException;
	
	/**
	 * Gets all cache names.
	 * 
	 * @return A list of cache names
	 * @throws Exception If an error occurs during the operation
	 */
	List<String> getCacheNames() throws EtcdServiceException;

	/**
	 * Ensures that a cache with the specified name exists in the etcd store.
	 * If the cache doesn't exist, it will be created and registered in the cache list.
	 * This method is typically called before performing operations on a cache to ensure
	 * the cache is properly initialized.
	 * 
	 * @param cacheName The name of the cache to ensure exists
	 * @throws EtcdServiceException If an error occurs during the operation
	 */
	void ensureCacheExists() throws EtcdServiceException;

	/**
	 * Gets all keys from the cache.
	 * 
	 * @return A list of all keys in the cache
	 * @throws EtcdServiceException If an error occurs during the operation
	 */
	Set<String> getAllKeys() throws EtcdServiceException;

	/**
	 * Deletes all keys in the cache in a single atomic operation. This method uses
	 * etcd's range delete functionality to efficiently remove all keys that match
	 * the cache's prefix.
	 *
	 * @throws EtcdServiceException if the deletion operation fails or if the
	 *                              operation is interrupted
	 */
	long deleteAllCacheKeys() throws EtcdServiceException;

	/**
	 * Gets a map of all values from cache.
	 */
	Map<String, T> getAll() throws EtcdServiceException;

} 