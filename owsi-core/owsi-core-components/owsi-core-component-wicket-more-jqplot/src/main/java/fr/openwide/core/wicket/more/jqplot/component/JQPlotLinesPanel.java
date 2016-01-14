package fr.openwide.core.wicket.more.jqplot.component;

import fr.openwide.core.wicket.more.jqplot.config.AbstractJQPlotConfigurer;
import fr.openwide.core.wicket.more.jqplot.data.adapter.IJQPlotDataAdapter;
import fr.openwide.core.wicket.more.jqplot.data.adapter.JQPlotDataAdapters;
import nl.topicus.wqplot.options.PlotOptions;

public class JQPlotLinesPanel<S, K, V extends Number & Comparable<V>> extends JQPlotPanel<S, K, V> {

	private static final long serialVersionUID = -5575918534912813908L;

	public JQPlotLinesPanel(String id, IJQPlotDataAdapter<S, K, V> dataAdapter) {
		super(id, JQPlotDataAdapters.fix(dataAdapter));
		
		add(new AbstractJQPlotConfigurer<S, K>() {
			private static final long serialVersionUID = 1L;
			
			@Override
			public void initialize(PlotOptions options) {
				options.getSeriesDefaults()
						.setRendererOptions(getOptionsFactory().newPlotLineRendererOptions());
			}
		});
	}
}
