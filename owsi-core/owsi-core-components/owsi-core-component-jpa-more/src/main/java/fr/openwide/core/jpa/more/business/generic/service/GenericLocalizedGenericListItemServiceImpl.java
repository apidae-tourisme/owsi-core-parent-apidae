package fr.openwide.core.jpa.more.business.generic.service;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import javax.persistence.NonUniqueResultException;

import com.querydsl.core.types.EntityPath;

import fr.openwide.core.jpa.exception.ServiceException;
import fr.openwide.core.jpa.more.business.generic.dao.IGenericLocalizedGenericListItemDao;
import fr.openwide.core.jpa.more.business.generic.model.EnabledFilter;
import fr.openwide.core.jpa.more.business.generic.model.GenericLocalizedGenericListItem;
import fr.openwide.core.jpa.more.business.localization.model.AbstractLocalizedText;

public abstract class GenericLocalizedGenericListItemServiceImpl<GE extends GenericLocalizedGenericListItem<?, T>, T extends AbstractLocalizedText>
		implements IGenericLocalizedGenericListItemService<GE, T> {
	
	private final IGenericLocalizedGenericListItemDao<GE, T> dao;
	
	public GenericLocalizedGenericListItemServiceImpl(IGenericLocalizedGenericListItemDao<GE, T> dao) {
		super();
		this.dao = dao;
	}
	
	@Override
	public <E extends GE> E getById(Class<E> clazz, Long id) {
		return dao.getById(clazz, id);
	}
	
	protected <E extends GE> E getByNaturalId(Class<E> clazz, Object naturalId) {
		return dao.getByNaturalId(clazz, naturalId);
	}
	
	@Override
	public <E extends GE> void create(E entity) {
		dao.save(entity);
	}

	@Override
	public <E extends GE> void update(E entity) {
		dao.update(entity);
	}

	@Override
	public <E extends GE> void delete(E entity) {
		dao.delete(entity);
	}

	@Override
	public <E extends GE> List<E> list(Class<E> clazz, Comparator<? super E> comparator) {
		return dao.list(clazz, EnabledFilter.ALL, comparator);
	}
	
	@Override
	public <E extends GE> List<E> listEnabled(Class<E> clazz, Comparator<? super E> comparator) {
		return dao.list(clazz, EnabledFilter.ENABLED_ONLY, comparator);
	}
	
	@Override
	public <E extends GE> List<E> list(Class<E> clazz, EnabledFilter enabledFilter, Comparator<? super E> comparator) {
		return dao.list(clazz, enabledFilter, comparator);
	}

	@Override
	public <E extends GE> long count(Class<E> clazz) {
		return dao.count(clazz);
	}
	
	@Override
	public <E extends GE> long count(Class<E> clazz, EnabledFilter enabledFilter) {
		return dao.count(clazz, enabledFilter);
	}
	
	@Override
	public <E extends GE> List<E> listByLocalizedLabel(EntityPath<E> source, Locale locale, String label) {
		return dao.listByLocalizedLabel(source, locale, label);
	}
	
	@Override
	public <E extends GE> E getByLocalizedLabel(EntityPath<E> source, Locale locale, String label) {
		Collection<E> matches = listByLocalizedLabel(source, locale, label);
		if (matches.isEmpty()) {
			return null;
		} else if (matches.size() > 1) {
			throw new NonUniqueResultException("Plusieurs '" + source.getAnnotatedElement().getClass().getSimpleName() + "' ont un libell?? identique");
		} else {
			return matches.iterator().next();
		}
	}
	
	@Override
	@Deprecated
	public <E extends GE> List<E> searchAutocomplete(String searchPattern, Class<E> clazz, Locale locale, int limit, int offset)
			throws ServiceException {
		return dao.searchAutocomplete(searchPattern, clazz, locale, limit, offset);
	}
	
	@Override
	public void flush() {
		dao.flush();
	}
	
	@Override
	public void clear() {
		dao.clear();
	}

}
