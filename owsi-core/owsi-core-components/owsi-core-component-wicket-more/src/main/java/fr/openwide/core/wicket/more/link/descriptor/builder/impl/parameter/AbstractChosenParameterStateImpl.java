package fr.openwide.core.wicket.more.link.descriptor.builder.impl.parameter;

import java.util.Collections;
import java.util.List;

import org.apache.wicket.model.IDetachable;
import org.bindgen.BindingRoot;
import org.bindgen.binding.AbstractBinding;

import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import fr.openwide.core.commons.util.binding.ICoreBinding;
import fr.openwide.core.wicket.more.link.descriptor.builder.impl.parameter.mapping.CollectionLinkParameterMappingEntry;
import fr.openwide.core.wicket.more.link.descriptor.builder.impl.parameter.mapping.SimpleLinkParameterMappingEntry;
import fr.openwide.core.wicket.more.link.descriptor.builder.state.parameter.chosen.ITwoMappableParameterOneChosenParameterState;
import fr.openwide.core.wicket.more.link.descriptor.builder.state.parameter.chosen.ITwoMappableParameterTwoChosenParameterState;
import fr.openwide.core.wicket.more.link.descriptor.builder.state.parameter.chosen.common.IChosenParameterState;
import fr.openwide.core.wicket.more.link.descriptor.builder.state.parameter.chosen.common.IOneChosenParameterState;
import fr.openwide.core.wicket.more.link.descriptor.builder.state.parameter.mapping.IAddedParameterMappingState;
import fr.openwide.core.wicket.more.link.descriptor.parameter.mapping.InjectOnlyLinkParameterMappingEntry;
import fr.openwide.core.wicket.more.link.descriptor.parameter.mapping.factory.ILinkParameterMappingEntryFactory;
import fr.openwide.core.wicket.more.link.descriptor.parameter.validator.ConditionLinkParameterValidator;
import fr.openwide.core.wicket.more.link.descriptor.parameter.validator.factory.ILinkParameterValidatorFactory;
import fr.openwide.core.wicket.more.markup.html.factory.IDetachableFactory;

/**
 * A base for implementing all interfaces that extend {@link IChosenParameterState}.
 * 
 * <p>In order to keep things simple, it is expected that multiple interfaces are implemented at once. For example,
 * the implementation for two available parameters will implement {@link ITwoMappableParameterOneChosenParameterState}
 * <strong>and</strong> {@link ITwoMappableParameterTwoChosenParameterState}. This implies that generics must be left aside
 * (we use raw types).
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractChosenParameterStateImpl<TSelf, TInitialState>
		implements IChosenParameterState, IOneChosenParameterState {
	
	private final List<Integer> parameterIndices;

	public AbstractChosenParameterStateImpl() {
		this.parameterIndices = Lists.newArrayList();
	}
	
	protected abstract LinkParameterTypeInformation<?> getParameterTypeInformation(int index);
	
	public List<Integer> getParameterIndices() {
		return Collections.unmodifiableList(parameterIndices);
	}
	
	private void addDynamicParameter(int index) {
		parameterIndices.add(index);
	}
	
	private int getFirstIndex() {
		return parameterIndices.get(0);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public IAddedParameterMappingState<TInitialState> map(String parameterName) {
		LinkParameterTypeInformation<?> typeInfo = getParameterTypeInformation(getFirstIndex());
		if (typeInfo.getTypeDescriptorSupplier().get().isCollection()) {
			return map(CollectionLinkParameterMappingEntry.factory(
					parameterName, typeInfo.getTypeDescriptorSupplier(), (Supplier) typeInfo.getEmptyValueSupplier()
			));
		} else {
			return map(SimpleLinkParameterMappingEntry.factory(
					parameterName, typeInfo.getTypeDescriptorSupplier()
			));
		}
	}

	@Override
	public abstract IAddedParameterMappingState<TInitialState> map(ILinkParameterMappingEntryFactory parameterMappingEntryFactory);

	@Override
	public IAddedParameterMappingState<TInitialState> renderInUrl(String parameterName) {
		return map(InjectOnlyLinkParameterMappingEntry.factory(parameterName));
	}

	@SuppressWarnings("unchecked")
	@Override
	public IAddedParameterMappingState<TInitialState> renderInUrl(String parameterName, ICoreBinding binding) {
		return map(InjectOnlyLinkParameterMappingEntry.factory(parameterName, binding));
	}

	@SuppressWarnings("unchecked")
	@Override
	public TInitialState validator(Predicate predicate) {
		// (ILinkParameterValidatorFactory) cast to raw-type needed in java 8
		return validator((ILinkParameterValidatorFactory) ConditionLinkParameterValidator.predicateFactory(predicate));
	}

	@Override
	public TInitialState permission(String permissionName) {
		return validator(ConditionLinkParameterValidator.anyPermissionFactory(ImmutableList.of(permissionName)));
	}

	@Override
	public TInitialState permission(String firstPermissionName, String... otherPermissionNames) {
		return validator(ConditionLinkParameterValidator.anyPermissionFactory(Lists.asList(firstPermissionName, otherPermissionNames)));
	}

	@SuppressWarnings("unchecked")
	@Override
	public TInitialState permission(BindingRoot binding, String firstPermissionName,
			String... otherPermissionNames) {
		// (ILinkParameterValidatorFactory) cast to raw-type needed in java 8
		return validator((ILinkParameterValidatorFactory) ConditionLinkParameterValidator.anyPermissionFactory(binding,
				Lists.asList(firstPermissionName, otherPermissionNames)));
	}

	@Override
	public abstract TInitialState validator(ILinkParameterValidatorFactory parameterValidatorFactory);

	@SuppressWarnings("unchecked")
	@Override
	public TInitialState validator(IDetachableFactory conditionFactory) {
		// (ILinkParameterValidatorFactory) cast to raw-type needed in java 8
		return validator((ILinkParameterValidatorFactory) ConditionLinkParameterValidator.fromConditionFactory(conditionFactory));
	}

	@Override
	public abstract IDetachable page(IDetachableFactory pageClassFactory);

	@Override
	public abstract IDetachable resource(IDetachableFactory resourceReferenceFactory);

	@Override
	public abstract IDetachable imageResource(IDetachableFactory resourceReferenceFactory);

	protected abstract TSelf thisAsTSelf();

	public TSelf andFirst() {
		addDynamicParameter(0);
		return thisAsTSelf();
	}

	public TSelf andSecond() {
		addDynamicParameter(1);
		return thisAsTSelf();
	}

	public TSelf andThird() {
		addDynamicParameter(2);
		return thisAsTSelf();
	}

	public TSelf andFourth() {
		addDynamicParameter(3);
		return thisAsTSelf();
	}

}
