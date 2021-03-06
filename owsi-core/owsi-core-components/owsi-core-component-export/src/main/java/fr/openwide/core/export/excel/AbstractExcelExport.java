/*
 * Copyright (C) 2009-2010 Open Wide
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
package fr.openwide.core.export.excel;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFPalette;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;

/**
 * <p>Classe abstraite permettant de construire un document Excel.</p>
 *
 * @author Open Wide
 */
public abstract class AbstractExcelExport {
	
	private static final String SHEET_DEFAULT_TITLE = "Untitled";
	private static final int SHEET_TITLE_MAX_LENGTH = 32;
	
	/**
	 * Document Workbook POI.
	 */
	protected Workbook workbook;
	
	/**
	 * Gestionnaire de formats
	 */
	protected DataFormat dataFormat;

	/**
	 * Registre des styles.
	 */
	protected Map<String, CellStyle> styleRegistry = new HashMap<String, CellStyle>();
	
	/**
	 * Registre des couleurs
	 */
	protected Map<Short, Color> colorRegistry = new HashMap<Short, Color>();
	
	/**
	 * Registre des polices
	 */
	protected Map<String, Font> fontRegistry = new HashMap<String, Font>();

	/**
	 * Composant d'instantiation ind??pendant du format HSSF/XSSF
	 */
	protected CreationHelper creationHelper;
	
	/**
	 * Taille du papier.
	 */
	private short paperSize = PrintSetup.A4_PAPERSIZE;
	
	/**
	 * Constructeur.
	 */
	public AbstractExcelExport(Workbook workbook) {
		this.workbook = workbook;
		this.creationHelper = this.workbook.getCreationHelper();
		this.dataFormat = this.creationHelper.createDataFormat();
	}
	
	/**
	 * Cr??e une feuille de calcul.
	 * 
	 * @param titre titre de la feuille de calcul
	 * @return feuille de calcul
	 */
	protected Sheet createSheet(String title) {
		String sheetTitle = SHEET_DEFAULT_TITLE;

		if (title != null) {
			if (title.length() > SHEET_TITLE_MAX_LENGTH + 1) {
				sheetTitle = title.substring(0, 31);
			} else {
				sheetTitle = title;
			}
		}

		Sheet sheet = workbook.createSheet(sheetTitle);
		sheet.getPrintSetup().setPaperSize(paperSize);
		
		return sheet;
	}

	/**
	 * Enregistre un style dans le registre des styles.
	 * 
	 * @param name nom du style
	 * @param style style
	 */
	protected final void registerStyle(String name, CellStyle style) {
		styleRegistry.put(name, style);
	}

	/**
	 * R??cup??re un style depuis le registre.
	 * 
	 * @param name nom du style
	 * @return style
	 */
	protected final CellStyle getStyle(String name) {
		if (styleRegistry.containsKey(name)) {
			return styleRegistry.get(name);
		} else {
			throw new IllegalArgumentException("Style " + name + " is not registered");
		}
	}

	/**
	 * Cl??ne les informations d'un style.
	 * 
	 * @param style style ?? copier
	 * @return style
	 */
	protected final CellStyle cloneStyle(CellStyle style) {
		CellStyle returnStyle = workbook.createCellStyle();

		returnStyle.cloneStyleFrom(style);

		return returnStyle;
	}

	/**
	 * Enregistre une police dans le registre des polices
	 * 
	 * @param name nom de la police
	 * @param font police
	 */
	protected final void registerFont(String name, Font font) {
		fontRegistry.put(name, font);
	}

	/**
	 * R??cup??re une police depuis le registre.
	 * 
	 * @param name nom de la police
	 * @return police
	 */
	protected final Font getFont(String name) {
		if (fontRegistry.containsKey(name)) {
			return fontRegistry.get(name);
		} else {
			throw new IllegalArgumentException("Font " + name + " is not registered");
		}
	}

	/**
	 * Enregistre une couleur dans le registre des couleurs
	 * 
	 * @param index index de la couleur
	 * @param hexaColor code hexad??cimal de la couleur
	 */
	protected final void registerColor(short index, String hexaColor) {
		if (hexaColor != null && hexaColor.matches("#[0-9a-fA-F]{6}")) {
			int red = Integer.parseInt(hexaColor.substring(1, 3), 16);
			int green = Integer.parseInt(hexaColor.substring(3, 5), 16);
			int blue = Integer.parseInt(hexaColor.substring(5, 7), 16);

			colorRegistry.put(index, new Color(red, green, blue));

			if (workbook instanceof HSSFWorkbook) {
				HSSFPalette palette = ((HSSFWorkbook) workbook).getCustomPalette();
				palette.setColorAtIndex(index, (byte) red, (byte) green, (byte) blue);
			}
		}
	}

	protected final void setFontColor(Font font, Map<Short, Color> colorRegistry, short color) {
		if (font instanceof XSSFFont && colorRegistry.containsKey(color)) {
			((XSSFFont) font).setColor(new XSSFColor(colorRegistry.get(color)));
		} else {
			font.setColor(color);
		}
	}
	
	protected final void setStyleFillForegroundColor(CellStyle style, Map<Short, Color> colorRegistry, short color) {
		if (style instanceof XSSFCellStyle && colorRegistry.containsKey(color)) {
			((XSSFCellStyle) style).setFillForegroundColor(new XSSFColor(colorRegistry.get(color)));
		} else {
			style.setFillForegroundColor(color);
		}
	}
	
	protected final void setStyleFillBackgroundColor(CellStyle style, Map<Short, Color> colorRegistry, short color) {
		if (style instanceof XSSFCellStyle && colorRegistry.containsKey(color)) {
			((XSSFCellStyle) style).setFillBackgroundColor(new XSSFColor(colorRegistry.get(color)));
		} else {
			style.setFillBackgroundColor(color);
		}
	}
	
	protected final void setStyleTopBorderColor(CellStyle style, Map<Short, Color> colorRegistry, short color) {
		if (style instanceof XSSFCellStyle && colorRegistry.containsKey(color)) {
			((XSSFCellStyle) style).setTopBorderColor(new XSSFColor(colorRegistry.get(color)));
		} else {
			style.setTopBorderColor(color);
		}
	}
	
	protected final void setStyleBottomBorderColor(CellStyle style, Map<Short, Color> colorRegistry, short color) {
		if (style instanceof XSSFCellStyle && colorRegistry.containsKey(color)) {
			((XSSFCellStyle) style).setBottomBorderColor(new XSSFColor(colorRegistry.get(color)));
		} else {
			style.setBottomBorderColor(color);
		}
	}
	
	protected final void setStyleLeftBorderColor(CellStyle style, Map<Short, Color> colorRegistry, short color) {
		if (style instanceof XSSFCellStyle && colorRegistry.containsKey(color)) {
			((XSSFCellStyle) style).setLeftBorderColor(new XSSFColor(colorRegistry.get(color)));
		} else {
			style.setLeftBorderColor(color);
		}
	}
	
	protected final void setStyleRightBorderColor(CellStyle style, Map<Short, Color> colorRegistry, short color) {
		if (style instanceof XSSFCellStyle && colorRegistry.containsKey(color)) {
			((XSSFCellStyle) style).setRightBorderColor(new XSSFColor(colorRegistry.get(color)));
		} else {
			style.setRightBorderColor(color);
		}
	}

	public void setPaperSize(short paperSize) {
		this.paperSize = paperSize;
	}

}
