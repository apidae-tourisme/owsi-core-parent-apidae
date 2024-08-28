/*
 * Copyright (C) 2009-2011 Open Wide
 * Contact: contact@openwide.fr
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.openwide.core.wicket.more.markup.html.basic;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.openwide.core.wicket.markup.html.basic.AbstractCoreLabel;
import fr.openwide.core.wicket.more.rendering.Renderer;
import fr.openwide.core.wicket.more.util.IDatePattern;

public class LocalDateLabel extends AbstractCoreLabel<LocalDateLabel> {

	private static final long serialVersionUID = 437934576864130364L;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(LocalDateLabel.class);

	public LocalDateLabel(String id, IModel<Date> model, IDatePattern datePattern, ZoneId fuseauHoraire) {
		super(id, Renderer.fromDatePattern(datePattern).asModel(adapterDateAuFuseau(model, fuseauHoraire)));
	}

	private static IModel<Date> adapterDateAuFuseau(IModel<Date> model, ZoneId fuseauHoraire) {
		Date dateAConvertir = model.getObject();
		if (dateAConvertir != null) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			if (!isFormatValid(dateAConvertir, dateFormat)) {
				dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
			}
			dateFormat.setTimeZone(TimeZone.getTimeZone(fuseauHoraire));
			try {
				Date dateConvertie = dateFormat.parse(dateAConvertir.toString());
				return Model.of(dateConvertie);
			} catch (ParseException e) {
				LOGGER.error("Erreur lors de l'adapation de la date {} au fuseau horaire {}.", dateAConvertir,
						fuseauHoraire, e);
				return model;
			}
		} else {
			return model;
		}
	}

	private static boolean isFormatValid(Date date, SimpleDateFormat format) {
		try {
			format.parse(date.toString());
			return true;
		} catch (ParseException e) {
			return false;
		}
	}

	@Override
	protected LocalDateLabel thisAsT() {
		return this;
	}

}
