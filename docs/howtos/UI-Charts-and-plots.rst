UI Charts and plots
===================

This page explains how to generate data charts using OWSI-Core.

JQPlot
------

About
~~~~~

JQplot is "a versatile and expandable JQuery plotting plugin" (see `the official site <http://www.jqplot.com/>`_).
OWSI-Core uses `WQPlot <https://github.com/openwide-java/wiquery-jqplot/>`_ in order to ensure low-level integration of JQPlot into wicket, plus a specific maven module (`fr.openwide.core.components:owsi-core-component-wicket-more-jqplot`) in order to provide easy-to-use, high-level integration.
Please note that WQPlot was originally `a wicketstuff project <https://github.com/wicketstuff/wiquery-jqplot>`_, which seems no longer maintained.

High-level usage
~~~~~~~~~~~~~~~~

Architecture
^^^^^^^^^^^^

You will encounter four types of components when you'll set up a chart:

* The data provider, which implements `IJQPlotDataProvider`. Its role is extracting data from your service layer (or an `IModel`, or anything you want) and provide it in a standardized form.
* The data adapter, which implements `IJQPlotDataAdapter`. Its role is to take data from a data provider and transform it in the WQPlot types (`BaseSeries`, `NumberSeries`, and so on) that can be used by the panel.
* The panel itself, which extends `JQPlotPanel`. It's a Wicket component that uses the data adapter and WQPlot in order to generate the actual chart.
* The configurers, which implement `IJQPlotConfigurer`. Their role is to customize various JQPlot options.

Using it in your application
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Setup global configuration
""""""""""""""""""""""""""

Add a dependency in your pom to `fr.openwide.core.components:owsi-core-component-wicket-more-jqplot`.

You will also need to import some Spring configuration: add an `@Import` annotation to your webapp in order to import `fr.openwide.core.wicket.more.jqplot.config.JQPlotJavaConfig`.

You may optionally, instead of importing `JQPlotJavaConfig` directly, define you own configuration that extend `JQPlotJavaConfig`. In this case, you will be able to override the default options passed to JQPlot's plots.

Setup your chart
""""""""""""""""

First, setup your data source. Generally it will be a service that returns:

* A `java.util.Map<K,V>` if there's only one data series.
* A `com.google.common.collect.Table<S,K,V>` if there are multiple data series.

See `Querying#generating-maps-and-tables <Querying.html#generating-maps-and-tables>`_ for more information about how to easily generate a `Map` or `Table` from a QueryDSL query in your DAO.

Then, pick your data provider:

* `fr.openwide.core.wicket.more.jqplot.data.provider.JQPlotMapDataProvider<S, K, V>` if you've got a service that returns a `Map`. Then you will wrap the service call in a `LoadableDetachableModel`, and pass the model as a constructor parameter to the data provider.
* `fr.openwide.core.wicket.more.jqplot.data.provider.JQPlotTableDataProvider<S, K, V>` if you've got a service that returns a `Table`.
* Or, if for some reason the above won't do, you may implement your own.

Then, pick your chart:

* For bar charts, use `fr.openwide.core.wicket.more.jqplot.component.JQPlotBarsPanel<S, K, V>`
* For line charts, use `fr.openwide.core.wicket.more.jqplot.component.JQPlotLinesPanel<S, K, V>`
* For pie charts, use `fr.openwide.core.wicket.more.jqplot.component.JQPlotPiePanel<K, V>`
* For stacked bar charts, use `fr.openwide.core.wicket.more.jqplot.component.JQPlotStackedBarsPanel<S, K, V extends Number & Comparable<V>>`
* For stacked lines charts, use `fr.openwide.core.wicket.more.jqplot.component.JQPlotStackedLinesPanel<S, K, V>`

Then, pick your data adapter (it will be the bridge between your panel and your data provider):

* If your data references keys from a discrete domain (i.e. with a small, finite number of values in the observed range), use`fr.openwide.core.wicket.more.jqplot.data.adapter.JQPlotDiscreteKeysDataAdapter<S, K, V>`. Please keep in mind that if you want your X axis to have a linear scale, you should either ensure that all keys are represented in your data, or provide to the data adapter a model containing all of the expected keys (so that the adapter can generate a linear axis).
* If, on the other hand, your data references keys from a continuous domain (i.e. with a high or infinite number of values in the observed range), use one of these:

   * `fr.openwide.core.wicket.more.jqplot.data.adapter.JQPlotContinuousDateKeysDataAdapter<S, K, V>`
   * `fr.openwide.core.wicket.more.jqplot.data.adapter.JQPlotContinuousNumberKeysDataAdapter<S, K extends Number, V extends Number>`

* Or, if for some reason the above won't do, you may implement your own.

Note: pie charts are a special case: you won't need a data adapter, just a data provider.

And finally, put all of this together: you've got a basic chart. Some examples are provided in the "Statistics" page of OWSI-Core's wicket showcase (`fr.openwide.core.showcase:wicket-showcase`).

To go further:

* You may add one or several configurers to the chart in order to customize JQPlot options: either pick a configurer from `fr.openwide.core.wicket.more.jqplot.config.JQPlotConfigurers` or use your own implementations.
* You may wrap your data adapter in order to customize the data passed to JQPlot: see `fr.openwide.core.wicket.more.jqplot.data.adapter.JQPlotDataAdapters`. You may for instance add a percentage in the tooltip shown when hovering over a stacked bar.

Low-level usage
~~~~~~~~~~~~~~~

If `fr.openwide.core.components:owsi-core-component-wicket-more-jqplot` is not flexible enough for your needs, you may still use raw wqplot. Some examples are provided in the "Statistics" page of OWSI-Core's wicket showcase (`fr.openwide.core.showcase:wicket-showcase`).

Keep in mind that you'll need to explicitly update jqplot options whenever the data changes (the X axis ticks, for instance, if you declared them explicitly).

Troubleshooting
~~~~~~~~~~~~~~~

The plots are not drawn
^^^^^^^^^^^^^^^^^^^^^^^

Please be aware that the supporting markup of your plots must be initially visible (i.e. not `display: none`) if you want the plots to be drawn automatically. If, for instance, your plots are located in an initially inactive (hidden) tab, then they will not be drawn automatically.

To address the specific case of plots in an initially inactive Bootstrap tab, you may use the following snippet:

.. code-block:: java

   tabContainer.add(new JQPlotReplotBehavior("shown.bs.tab"));

This will ensure that plots are "replotted" each time the user switches tabs.

Other plotting libraries
------------------------

No other plotting library is integrated into OWSI-Core at the moment.

