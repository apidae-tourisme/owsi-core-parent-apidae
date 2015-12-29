!function( $, window, document, undefined ) {
	
	"use strict";
	
	var MultivaluedExpand = function (element, options) {
		this.init('multivaluedExpand', element, options);
	};
	
	MultivaluedExpand.prototype = {
		
		constructor: MultivaluedExpand,
		
		init : function(type, element, options) {
			this.type = type;
			this.$element = $(element);
			this.options = this.getOptions(options);
			
			var elementContainer = this.$element;
			var multivaluedContainer = elementContainer.hasClass('multivalued') ? elementContainer : elementContainer.closest(".multivalued");
			var items = $(".multivalued-item", multivaluedContainer);
			
			if (items.length > 1) {
				var toogleButton = $(this.options.toggleButtonHtml).addClass('expand-toggle');
				toogleButton.on('click', function() {
					multivaluedContainer.toggleClass('closed');
				});
				
				elementContainer.prepend(toogleButton);
				multivaluedContainer.addClass('closed');
			}
		},
		
		getOptions: function (options) {
			options = $.extend({}, $.fn[this.type].defaults, options, this.$element.data());
			return options;
		},
		
	};
	
	$.fn.multivaluedExpand = function (option) {
		return this.each(function () {
			var $this = $(this)
				, data = $this.data('multivaluedExpand')
				, options = typeof option == 'object' && option;
			if (!data) {
				$this.data('multivaluedExpand', (data = new MultivaluedExpand(this, options)));
			}
			if (typeof option == 'string') {
				data[option]();
			}
		});
	};
	
	$.fn.multivaluedExpand.Constructor = MultivaluedExpand;
	
	$.fn.multivaluedExpand.defaults = {
			toggleButtonHtml : '<a><span class="fa fa-plus-circle" /><span class="fa fa-minus-circle" /></a>'
	};
}(window.jQuery, window, document);