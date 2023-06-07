package fr.openwide.core.jpa.more.business.task.search;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.querydsl.core.types.OrderSpecifier;

import fr.openwide.core.jpa.more.business.sort.ISort;
import fr.openwide.core.jpa.more.business.sort.SortUtils;
import fr.openwide.core.jpa.more.business.task.model.QQueuedTaskHolder;

public enum QueuedTaskHolderSort implements ISort<OrderSpecifier<?>> {
	
	CREATION_DATE {
		@Override
		public List<OrderSpecifier<?>> getSortFields(SortOrder sortOrder) {
			return ImmutableList.of(
					SortUtils.orderSpecifier(this, sortOrder, QQueuedTaskHolder.queuedTaskHolder.endDate),
					SortUtils.orderSpecifier(this, sortOrder, QQueuedTaskHolder.queuedTaskHolder.startDate),
					SortUtils.orderSpecifier(this, sortOrder, QQueuedTaskHolder.queuedTaskHolder.creationDate)
			);
		}
		
		@Override
		public SortOrder getDefaultOrder() {
			return SortOrder.DESC;
		}
	},
	NAME {
		@Override
		public List<OrderSpecifier<?>> getSortFields(SortOrder sortOrder) {
			return ImmutableList.of(
					SortUtils.orderSpecifier(this, sortOrder, QQueuedTaskHolder.queuedTaskHolder.name)
			);
		}
		
		@Override
		public SortOrder getDefaultOrder() {
			return SortOrder.ASC;
		}
	},
	ID {
		@Override
		public List<OrderSpecifier<?>> getSortFields(SortOrder sortOrder) {
			return ImmutableList.of(
					SortUtils.orderSpecifier(this, sortOrder, QQueuedTaskHolder.queuedTaskHolder.id)
			);
		}
		
		@Override
		public SortOrder getDefaultOrder() {
			return SortOrder.DESC;
		}
	};
	
	@Override
	public abstract SortOrder getDefaultOrder();
	
	@Override
	public abstract List<OrderSpecifier<?>> getSortFields(SortOrder sortOrder);
}
