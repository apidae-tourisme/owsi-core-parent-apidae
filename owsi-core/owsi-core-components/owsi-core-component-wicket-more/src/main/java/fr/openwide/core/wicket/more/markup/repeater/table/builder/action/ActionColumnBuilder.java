package fr.openwide.core.wicket.more.markup.repeater.table.builder.action;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.model.IModel;
import org.springframework.security.acls.model.Permission;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fr.openwide.core.commons.util.binding.ICoreBinding;
import fr.openwide.core.jpa.more.business.sort.ISort;
import fr.openwide.core.wicket.more.condition.Condition;
import fr.openwide.core.wicket.more.link.descriptor.generator.ILinkGenerator;
import fr.openwide.core.wicket.more.link.descriptor.mapper.BindingOneParameterLinkDescriptorMapper;
import fr.openwide.core.wicket.more.link.descriptor.mapper.ILinkDescriptorMapper;
import fr.openwide.core.wicket.more.markup.html.action.IOneParameterAction;
import fr.openwide.core.wicket.more.markup.html.action.IOneParameterAjaxAction;
import fr.openwide.core.wicket.more.markup.html.bootstrap.label.renderer.BootstrapRenderer;
import fr.openwide.core.wicket.more.markup.html.factory.ConditionFactories;
import fr.openwide.core.wicket.more.markup.html.factory.IOneParameterComponentFactory;
import fr.openwide.core.wicket.more.markup.html.template.js.jquery.plugins.bootstrap.confirm.component.AjaxConfirmLink;
import fr.openwide.core.wicket.more.markup.repeater.table.builder.DataTableBuilder;
import fr.openwide.core.wicket.more.markup.repeater.table.builder.action.factory.ActionColumnActionFactory;
import fr.openwide.core.wicket.more.markup.repeater.table.builder.action.factory.ActionColumnAjaxActionFactory;
import fr.openwide.core.wicket.more.markup.repeater.table.builder.action.state.IActionColumnAddedActionState;
import fr.openwide.core.wicket.more.markup.repeater.table.builder.action.state.IActionColumnAddedAjaxActionState;
import fr.openwide.core.wicket.more.markup.repeater.table.builder.action.state.IActionColumnAddedConfirmActionState;
import fr.openwide.core.wicket.more.markup.repeater.table.builder.action.state.IActionColumnAddedElementState;
import fr.openwide.core.wicket.more.markup.repeater.table.builder.action.state.IActionColumnAddedLinkState;
import fr.openwide.core.wicket.more.markup.repeater.table.builder.action.state.IActionColumnBuildState;
import fr.openwide.core.wicket.more.markup.repeater.table.builder.action.state.IActionColumnConfirmActionBuilderStepStart;
import fr.openwide.core.wicket.more.markup.repeater.table.builder.state.IAddedCoreColumnState;
import fr.openwide.core.wicket.more.markup.repeater.table.column.CoreActionColumn;

public class ActionColumnBuilder<T, S extends ISort<?>> implements IActionColumnBuildState<T, S> {

	private final DataTableBuilder<T, S> dataTableBuilder;

	private final IModel<String> headerLabelModel;

	private final Set<String> cssClassesOnElements = Sets.newHashSet();

	private final List<AbstractActionColumnElementBuilder<T, ?, ?>> builders = Lists.newArrayList();

	public ActionColumnBuilder(DataTableBuilder<T, S> dataTableBuilder, IModel<String> headerLabelModel) {
		this.dataTableBuilder = dataTableBuilder;
		this.headerLabelModel = headerLabelModel;
	}

	private abstract class ActionColumnBuilderWrapper implements IActionColumnBuildState<T, S> {
		
		@Override
		public IActionColumnAddedLinkState<T, S> addLink(BootstrapRenderer<? super T> renderer,
				ILinkDescriptorMapper<? extends ILinkGenerator, ? super IModel<T>> mapper) {
			return ActionColumnBuilder.this.addLink(renderer, mapper);
		}
		
		@Override
		public <C> IActionColumnAddedLinkState<T, S> addLink(BootstrapRenderer<? super T> renderer,
				ICoreBinding<? super T, C> binding,
				ILinkDescriptorMapper<? extends ILinkGenerator, ? super IModel<C>> mapper) {
			return ActionColumnBuilder.this.addLink(renderer, binding, mapper);
		}
		
		@Override
		public IActionColumnAddedLinkState<T, S> addLabelledLink(BootstrapRenderer<? super T> renderer,
				ILinkDescriptorMapper<? extends ILinkGenerator, ? super IModel<T>> mapper) {
			return ActionColumnBuilder.this.addLabelledLink(renderer, mapper);
		}
		
		@Override
		public <C> IActionColumnAddedLinkState<T, S> addLabelledLink(BootstrapRenderer<? super T> renderer,
				ICoreBinding<? super T, C> binding,
				ILinkDescriptorMapper<? extends ILinkGenerator, ? super IModel<C>> mapper) {
			return ActionColumnBuilder.this.addLabelledLink(renderer, binding, mapper);
		}
		
		@Override
		public IActionColumnAddedAjaxActionState<T, S> addAction(BootstrapRenderer<? super T> renderer,
				IOneParameterAjaxAction<? super IModel<T>> action) {
			return ActionColumnBuilder.this.addAction(renderer, action);
		}
		
		@Override
		public IActionColumnAddedAjaxActionState<T, S> addLabelledAction(BootstrapRenderer<? super T> renderer,
				IOneParameterAjaxAction<? super IModel<T>> action) {
			return ActionColumnBuilder.this.addLabelledAction(renderer, action);
		}
		
		@Override
		public IActionColumnAddedActionState<T, S> addAction(BootstrapRenderer<? super T> renderer,
				IOneParameterAction<? super IModel<T>> action) {
			return ActionColumnBuilder.this.addAction(renderer, action);
		}
		
		@Override
		public IActionColumnAddedActionState<T, S> addLabelledAction(BootstrapRenderer<? super T> renderer,
				IOneParameterAction<? super IModel<T>> action) {
			return ActionColumnBuilder.this.addLabelledAction(renderer, action);
		}
		
		@Override
		public IActionColumnConfirmActionBuilderStepStart<T, S> addConfirmAction(BootstrapRenderer<? super T> renderer) {
			return ActionColumnBuilder.this.addConfirmAction(renderer);
		}
		
		@Override
		public IActionColumnAddedActionState<T, S> addAction(BootstrapRenderer<? super T> renderer,
				IOneParameterComponentFactory<? extends AbstractLink, IModel<T>> factory) {
			return ActionColumnBuilder.this.addAction(renderer, factory);
		}
		
		@Override
		public IActionColumnAddedActionState<T, S> addLabelledAction(BootstrapRenderer<? super T> renderer,
				IOneParameterComponentFactory<? extends AbstractLink, IModel<T>> factory) {
			return ActionColumnBuilder.this.addLabelledAction(renderer, factory);
		}
		
		@Override
		public IActionColumnBuildState<T, S> withClassOnElements(String cssClassOnElements) {
			return ActionColumnBuilder.this.withClassOnElements(cssClassOnElements);
		}
		
		@Override
		public IAddedCoreColumnState<T, S> end() {
			return ActionColumnBuilder.this.end();
		}
		
	}

	private abstract class ActionColumnAddedElementState<NextState extends IActionColumnAddedElementState<T, S>>
			extends ActionColumnBuilderWrapper implements IActionColumnAddedElementState<T, S> {
		
		protected abstract NextState getNextState();
		
		protected abstract AbstractActionColumnElementBuilder<T, ?, ?> getElementBuilder();
		
		@Override
		public NextState showLabel() {
			getElementBuilder().showLabel();
			return getNextState();
		}
		
		@Override
		public NextState showLabel(Condition showLabelCondition) {
			getElementBuilder().showLabel(showLabelCondition);
			return getNextState();
		}
		
		@Override
		public NextState hideLabel() {
			getElementBuilder().hideLabel();
			return getNextState();
		}
		
		@Override
		public NextState hideLabel(Condition hideLabelCondition) {
			getElementBuilder().hideLabel(hideLabelCondition);
			return getNextState();
		}
		
		@Override
		public NextState showTooltip() {
			getElementBuilder().showTooltip();
			return getNextState();
		}
		
		@Override
		public NextState showTooltip(Condition showTooltipCondition) {
			getElementBuilder().showTooltip(showTooltipCondition);
			return getNextState();
		}
		
		@Override
		public NextState hideTooltip() {
			getElementBuilder().hideTooltip();
			return getNextState();
		}
		
		@Override
		public NextState hideTooltip(Condition hideTooltipCondition) {
			getElementBuilder().hideTooltip(hideTooltipCondition);
			return getNextState();
		}
		
		@Override
		public NextState showIcon() {
			getElementBuilder().showIcon();
			return getNextState();
		}
		
		@Override
		public NextState showIcon(Condition showIconCondition) {
			getElementBuilder().showIcon(showIconCondition);
			return getNextState();
		}
		
		@Override
		public NextState hideIcon() {
			getElementBuilder().hideIcon();
			return getNextState();
		}
		
		@Override
		public NextState hideIcon(Condition hideIconCondition) {
			getElementBuilder().hideIcon(hideIconCondition);
			return getNextState();
		}
		
		@Override
		public NextState showPlaceholder() {
			getElementBuilder().showPlaceholder();
			return getNextState();
		}
		
		@Override
		public NextState showPlaceholder(Condition showPlaceholderCondition) {
			getElementBuilder().showPlaceholder(showPlaceholderCondition);
			return getNextState();
		}
		
		@Override
		public NextState hidePlaceholder() {
			getElementBuilder().hidePlaceholder();
			return getNextState();
		}
		
		@Override
		public NextState hidePlaceholder(Condition hidePlaceholderCondition) {
			getElementBuilder().hidePlaceholder(hidePlaceholderCondition);
			return getNextState();
		}
		
		@Override
		public NextState when(final Condition condition) {
			getElementBuilder().addConditionFactory(ConditionFactories.constant(condition));
			return getNextState();
		}
		
		@Override
		public NextState when(final Predicate<? super T> predicate) {
			getElementBuilder().addConditionFactory(ConditionFactories.predicate(predicate));
			return getNextState();
		}
		
		@Override
		public NextState whenPermission(final String permission) {
			getElementBuilder().addConditionFactory(ConditionFactories.<T>permission(permission));
			return getNextState();
		}
		
		@Override
		public NextState whenPermission(final Permission permission) {
			getElementBuilder().addConditionFactory(ConditionFactories.<T>permission(permission));
			return getNextState();
		}
		
		@Override
		public NextState withClass(String cssClass) {
			getElementBuilder().addCssClass(cssClass);
			return getNextState();
		}
		
		@Override
		public NextState add(Behavior...behaviors) {
			getElementBuilder().addBehaviors(behaviors);
			return getNextState();
		}
	}

	private class ActionColumnAddedLinkState extends ActionColumnAddedElementState<IActionColumnAddedLinkState<T, S>>
			implements IActionColumnAddedLinkState<T, S> {
		
		private final ActionColumnLinkBuilder<T> elementBuilder;
		
		public ActionColumnAddedLinkState(ActionColumnLinkBuilder<T> elementBuilder) {
			super();
			this.elementBuilder = Objects.requireNonNull(elementBuilder);
		}
		
		@Override
		public ActionColumnLinkBuilder<T> getElementBuilder() {
			return elementBuilder;
		}
		
		@Override
		protected IActionColumnAddedLinkState<T, S> getNextState() {
			return this;
		}

		@Override
		public IActionColumnAddedLinkState<T, S> hideIfInvalid() {
			getElementBuilder().hideIfInvalid();
			return getNextState();
		}
	}

	private class ActionColumnAddedAjaxActionState extends ActionColumnAddedElementState<IActionColumnAddedAjaxActionState<T, S>>
			implements IActionColumnAddedAjaxActionState<T, S> {
		
		private final AbstractActionColumnElementBuilder<T, ?, ?> elementBuilder;
		
		public ActionColumnAddedAjaxActionState(AbstractActionColumnElementBuilder<T, ?, ?> elementBuilder) {
			super();
			this.elementBuilder = Objects.requireNonNull(elementBuilder);
		}
		
		@Override
		public AbstractActionColumnElementBuilder<T, ?, ?> getElementBuilder() {
			return elementBuilder;
		}
		
		@Override
		protected IActionColumnAddedAjaxActionState<T, S> getNextState() {
			return this;
		}
	}

	private class ActionColumnAddedActionState extends ActionColumnAddedElementState<IActionColumnAddedActionState<T, S>>
			implements IActionColumnAddedActionState<T, S> {
		
		private final AbstractActionColumnElementBuilder<T, ?, ?> elementBuilder;
		
		public ActionColumnAddedActionState(AbstractActionColumnElementBuilder<T, ?, ?> elementBuilder) {
			super();
			this.elementBuilder = Objects.requireNonNull(elementBuilder);
		}
		
		@Override
		public AbstractActionColumnElementBuilder<T, ?, ?> getElementBuilder() {
			return elementBuilder;
		}
		
		@Override
		protected IActionColumnAddedActionState<T, S> getNextState() {
			return this;
		}
	}

	private class ActionColumnAddedConfirmActionState extends ActionColumnAddedElementState<IActionColumnAddedConfirmActionState<T, S>>
			implements IActionColumnAddedConfirmActionState<T, S> {
		
		private final AbstractActionColumnElementBuilder<T, ?, ?> elementBuilder;
		
		public ActionColumnAddedConfirmActionState(AbstractActionColumnElementBuilder<T, ?, ?> elementBuilder) {
			super();
			this.elementBuilder = Objects.requireNonNull(elementBuilder);
		}
		
		@Override
		public AbstractActionColumnElementBuilder<T, ?, ?> getElementBuilder() {
			return elementBuilder;
		}
		
		@Override
		protected IActionColumnAddedConfirmActionState<T, S> getNextState() {
			return this;
		}
	}

	@Override
	public IActionColumnAddedLinkState<T, S> addLink(BootstrapRenderer<? super T> renderer,
			ILinkDescriptorMapper<? extends ILinkGenerator, ? super IModel<T>> mapper) {
		ActionColumnLinkBuilder<T> factory = new ActionColumnLinkBuilder<>(renderer, mapper);
		builders.add(factory);
		return new ActionColumnAddedLinkState(factory);
	}

	@Override
	public <C> IActionColumnAddedLinkState<T, S> addLink(BootstrapRenderer<? super T> renderer,
			ICoreBinding<? super T, C> binding,
			ILinkDescriptorMapper<? extends ILinkGenerator, ? super IModel<C>> mapper) {
		return addLink(renderer, new BindingOneParameterLinkDescriptorMapper<>(binding, mapper));
	}

	@Override
	public IActionColumnAddedLinkState<T, S> addLabelledLink(BootstrapRenderer<? super T> renderer,
			ILinkDescriptorMapper<? extends ILinkGenerator, ? super IModel<T>> mapper) {
		return addLink(renderer, mapper).showLabel();
	}

	@Override
	public <C> IActionColumnAddedLinkState<T, S> addLabelledLink(BootstrapRenderer<? super T> renderer,
			ICoreBinding<? super T, C> binding,
			ILinkDescriptorMapper<? extends ILinkGenerator, ? super IModel<C>> mapper) {
		return addLabelledLink(renderer, new BindingOneParameterLinkDescriptorMapper<>(binding, mapper));
	}

	@Override
	public IActionColumnAddedAjaxActionState<T, S> addAction(BootstrapRenderer<? super T> renderer,
			IOneParameterAjaxAction<? super IModel<T>> action) {
		AbstractActionColumnElementBuilder<T, ?, ?> builder =
				new ActionColumnSimpleElementBuilder<>(renderer, new ActionColumnAjaxActionFactory<T>(action));
		builders.add(builder);
		return new ActionColumnAddedAjaxActionState(builder);
	}

	@Override
	public IActionColumnAddedAjaxActionState<T, S> addLabelledAction(BootstrapRenderer<? super T> renderer,
			IOneParameterAjaxAction<? super IModel<T>> action) {
		return addAction(renderer, action).showLabel();
	}

	@Override
	public IActionColumnAddedActionState<T, S> addAction(BootstrapRenderer<? super T> renderer,
			IOneParameterAction<? super IModel<T>> action) {
		return addAction(renderer, new ActionColumnActionFactory<T>(action));
	}

	@Override
	public IActionColumnAddedActionState<T, S> addLabelledAction(BootstrapRenderer<? super T> renderer,
			IOneParameterAction<? super IModel<T>> action) {
		return addAction(renderer, action).showLabel();
	}

	@Override
	public IActionColumnConfirmActionBuilderStepStart<T, S> addConfirmAction(BootstrapRenderer<? super T> renderer) {
		return new ActionColumnConfirmActionBuilder<>(this, renderer);
	}

	public IActionColumnAddedConfirmActionState<T, S> addConfirmAction(BootstrapRenderer<? super T> renderer,
			IOneParameterComponentFactory<AjaxConfirmLink<T>, IModel<T>> ajaxConfirmLinkFactory) {
		AbstractActionColumnElementBuilder<T, ?, ?> builder = new ActionColumnSimpleElementBuilder<>(renderer, ajaxConfirmLinkFactory);
		builders.add(builder);
		return new ActionColumnAddedConfirmActionState(builder);
	}
	
	@Override
	public IActionColumnAddedActionState<T, S> addAction(BootstrapRenderer<? super T> renderer,
			IOneParameterComponentFactory<? extends AbstractLink, IModel<T>> factory) {
		AbstractActionColumnElementBuilder<T, ?, ?> builder =
				new ActionColumnSimpleElementBuilder<>(renderer, factory);
		builders.add(builder);
		return new ActionColumnAddedActionState(builder);
	}
	
	@Override
	public IActionColumnAddedActionState<T, S> addLabelledAction(BootstrapRenderer<? super T> renderer,
			IOneParameterComponentFactory<? extends AbstractLink, IModel<T>> factory) {
		return addAction(renderer, factory).showLabel();
	}
	

	@Override
	public IActionColumnBuildState<T, S> withClassOnElements(String cssClassOnElements) {
		cssClassesOnElements.add(cssClassOnElements);
		return this;
	}

	public String getCssClassOnElements() {
		return Joiner.on(" ").join(cssClassesOnElements);
	}

	@Override
	public IAddedCoreColumnState<T, S> end() {
		for (AbstractActionColumnElementBuilder<T, ?, ?> builder : builders) {
			builder.addCssClass(getCssClassOnElements());
		}
		return dataTableBuilder.addActionColumn(new CoreActionColumn<T, S>(headerLabelModel, builders));
	}

}
