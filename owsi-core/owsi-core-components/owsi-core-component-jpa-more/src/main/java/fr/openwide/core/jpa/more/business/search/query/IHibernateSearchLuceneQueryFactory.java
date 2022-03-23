package fr.openwide.core.jpa.more.business.search.query;

import java.util.Collection;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.Query;
import fr.openwide.core.commons.util.binding.ICoreBinding;
import org.hibernate.search.query.dsl.QueryBuilder;

public interface IHibernateSearchLuceneQueryFactory {

	Analyzer getAnalyzer(Class<?> clazz);

	Analyzer getDefaultAnalyzer();

	QueryBuilder getQueryBuilder(Class<?> clazz);

	QueryBuilder getDefaultQueryBuilder();

	void setDefaultClass(Class<?> defaultClass);

	Class<?> getDefaultClass();

	Query any(QueryBuilder builder, Query ... queries);

	Query any(Query ... queries);

	Query all(QueryBuilder builder, Query ... queries);

	Query all(Query ... queries);

	Query matchNull(ICoreBinding<?, ?> binding);

	Query matchNull(QueryBuilder builder, ICoreBinding<?, ?> binding);

	Query matchNull(String fieldPath);

	Query matchNull(QueryBuilder builder, String fieldPath);

	<P> Query matchIfGiven(ICoreBinding<?, P> binding, P value);

	<P> Query matchIfGiven(QueryBuilder builder, ICoreBinding<?, P> binding, P value);

	<P> Query matchIfGiven(String fieldPath, P value);

	<P> Query matchIfGiven(QueryBuilder builder, String fieldPath, P value);

	Query matchOneTermIfGiven(ICoreBinding<?, String> binding, String terms);

	Query matchOneTermIfGiven(QueryBuilder builder, ICoreBinding<?, String> binding, String terms);

	Query matchOneTermIfGiven(String fieldPath, String terms);

	Query matchOneTermIfGiven(QueryBuilder builder, String fieldPath, String terms);

	Query matchAllTermsIfGiven(Analyzer analyzer, String terms, ICoreBinding<?, String> binding,
			@SuppressWarnings("unchecked") ICoreBinding<?, String> ... otherBindings);

	Query matchAllTermsIfGiven(String terms, ICoreBinding<?, String> binding,
			@SuppressWarnings("unchecked") ICoreBinding<?, String> ... otherBindings);

	Query matchAllTermsIfGiven(String terms, Iterable<String> fieldPaths);

	Query matchAllTermsIfGiven(Analyzer analyzer, String terms, Iterable<String> fieldPaths);

	Query matchAutocompleteIfGiven(Analyzer analyzer, String terms, ICoreBinding<?, String> binding,
			@SuppressWarnings("unchecked") ICoreBinding<?, String> ... otherBindings);

	Query matchAutocompleteIfGiven(String terms, ICoreBinding<?, String> binding,
			@SuppressWarnings("unchecked") ICoreBinding<?, String> ... otherBindings);

	Query matchAutocompleteIfGiven(String terms, Iterable<String> fieldPaths);

	Query matchAutocompleteIfGiven(Analyzer analyzer, String terms, Iterable<String> fieldPaths);

	Query matchFuzzyIfGiven(Analyzer analyzer, String terms, Integer maxEditDistance, ICoreBinding<?, String> binding,
			@SuppressWarnings("unchecked") ICoreBinding<?, String> ... otherBindings);

	Query matchFuzzyIfGiven(String terms, Integer maxEditDistance, ICoreBinding<?, String> binding,
			@SuppressWarnings("unchecked") ICoreBinding<?, String> ... otherBindings);

	Query matchFuzzyIfGiven(String terms, Integer maxEditDistance, Iterable<String> fieldPaths);

	Query matchFuzzyIfGiven(Analyzer analyzer, String terms, Integer maxEditDistance, Iterable<String> fieldPaths);

	<P> Query beIncludedIfGiven(ICoreBinding<?, ? extends Collection<P>> binding, P value);

	<P> Query beIncludedIfGiven(QueryBuilder builder, ICoreBinding<?, ? extends Collection<P>> binding, P value);

	<P> Query beIncludedIfGiven(String fieldPath, P value);

	<P> Query beIncludedIfGiven(QueryBuilder builder, String fieldPath, P value);

	<P> Query matchOneIfGiven(ICoreBinding<?, P> binding, Collection<? extends P> possibleValues);

	<P> Query matchOneIfGiven(QueryBuilder builder, ICoreBinding<?, P> binding, Collection<? extends P> possibleValues);

	<P> Query matchOneIfGiven(String fieldPath, Collection<? extends P> possibleValues);

	<P> Query matchOneIfGiven(QueryBuilder builder, String fieldPath, Collection<? extends P> possibleValues);

	<P> Query matchAllIfGiven(ICoreBinding<?, ? extends Collection<P>> binding, Collection<? extends P> possibleValues);

	<P> Query matchAllIfGiven(QueryBuilder builder, ICoreBinding<?, ? extends Collection<P>> binding, Collection<? extends P> possibleValues);

	<P> Query matchAllIfGiven(String fieldPath, Collection<? extends P> possibleValues);

	<P> Query matchAllIfGiven(QueryBuilder builder, String fieldPath, Collection<? extends P> values);

	Query matchIfTrue(ICoreBinding<?, Boolean> binding, boolean value, Boolean mustMatch);

	Query matchIfTrue(QueryBuilder builder, ICoreBinding<?, Boolean> binding, boolean value, Boolean mustMatch);

	<P> Query matchIfTrue(String fieldPath, boolean value, Boolean mustMatch);

	Query matchIfTrue(QueryBuilder builder, String fieldPath, boolean value, Boolean mustMatch);

	<P> Query matchRangeMin(ICoreBinding<?, P> binding, P min);

	<P> Query matchRangeMin(QueryBuilder builder, ICoreBinding<?, P> binding, P min);

	<P> Query matchRangeMin(String fieldPath, P min);

	<P> Query matchRangeMin(QueryBuilder builder, String fieldPath, P min);

	<P> Query matchRangeMax(ICoreBinding<?, P> binding, P max);

	<P> Query matchRangeMax(QueryBuilder builder, ICoreBinding<?, P> binding, P max);

	<P> Query matchRangeMax(String fieldPath, P max);

	<P> Query matchRangeMax(QueryBuilder builder, String fieldPath, P max);

	<P> Query matchRange(ICoreBinding<?, P> binding, P min, P max);

	<P> Query matchRange(QueryBuilder builder, ICoreBinding<?, P> binding, P min, P max);

	<P> Query matchRange(String fieldPath, P min, P max);

	<P> Query matchRange(QueryBuilder builder, String fieldPath, P min, P max);

}
