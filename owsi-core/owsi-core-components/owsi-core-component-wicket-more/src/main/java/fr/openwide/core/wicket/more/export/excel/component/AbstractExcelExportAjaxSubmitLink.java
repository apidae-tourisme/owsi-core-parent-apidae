package fr.openwide.core.wicket.more.export.excel.component;

import static fr.openwide.core.spring.property.SpringPropertyIds.TMP_EXPORT_EXCEL_PATH;

import java.io.File;
import java.io.FileOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.openwide.core.commons.util.mime.MediaType;
import fr.openwide.core.jpa.exception.ServiceException;
import fr.openwide.core.spring.property.service.IPropertyService;
import fr.openwide.core.wicket.more.export.file.behavior.FileDeferredDownloadBehavior;
import fr.openwide.core.wicket.more.export.util.ExportFileUtils;
import fr.openwide.core.wicket.more.markup.html.feedback.FeedbackUtils;
import fr.openwide.core.wicket.more.util.model.Detachables;

public abstract class AbstractExcelExportAjaxSubmitLink extends AjaxSubmitLink {
	
	private static final long serialVersionUID = -3122955700569654875L;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractExcelExportAjaxSubmitLink.class);
	
	@SpringBean
	private IPropertyService propertyService;
	
	private final ExcelExportWorkInProgressModalPopupPanel loadingPopup;
	
	private final FileDeferredDownloadBehavior ajaxDownload;
	
	private final IModel<File> tempFileModel = new Model<File>();
	
	private final IModel<MediaType> mediaTypeModel = new Model<MediaType>();
	
	public AbstractExcelExportAjaxSubmitLink(String id, Form<?> form, ExcelExportWorkInProgressModalPopupPanel loadingPopup, String fileNamePrefix) {
		this(id, form, loadingPopup, Model.of(fileNamePrefix));
	}

	public AbstractExcelExportAjaxSubmitLink(String id, Form<?> form, ExcelExportWorkInProgressModalPopupPanel loadingPopup, IModel<String> fileNamePrefixModel) {
		super(id, form);
		this.loadingPopup = loadingPopup;
		this.ajaxDownload = new FileDeferredDownloadBehavior(tempFileModel, ExportFileUtils.getFileNameMediaTypeModel(fileNamePrefixModel, mediaTypeModel));
		
		add(ajaxDownload);
	}
	
	protected MediaType getMediaType(Workbook workbook) {
		if (workbook instanceof HSSFWorkbook) {
			return MediaType.APPLICATION_MS_EXCEL;
		} else if (workbook instanceof XSSFWorkbook) {
			return MediaType.APPLICATION_OPENXML_EXCEL;
		} else if (workbook instanceof SXSSFWorkbook) {
			return MediaType.APPLICATION_OPENXML_EXCEL;
		} else {
			// Default
			return MediaType.APPLICATION_MS_EXCEL;
		}
	}
	
	@Override
	protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
		super.updateAjaxAttributes(attributes);
		loadingPopup.updateAjaxAttributes(attributes);
	}
	
	@Override
	public void onSubmit(AjaxRequestTarget target, Form<?> form) {
		boolean hasError = false;
		File tmp = null;
		try {
			Workbook workbook = generateWorkbook();
			if (workbook.getNumberOfSheets() == 0) {
				onEmptyExport(workbook);
				hasError = true;
			} else {
				mediaTypeModel.setObject(getMediaType(workbook));
				
				tmp = File.createTempFile("export-", "", propertyService.get(TMP_EXPORT_EXCEL_PATH));
				tempFileModel.setObject(tmp);
				FileOutputStream output = new FileOutputStream(tmp);
				workbook.write(output);
				output.close();
			}
		} catch (Exception e) {
			LOGGER.error("Erreur en g??n??rant un fichier Excel.", e);
			
			hasError = true;
			FileUtils.deleteQuietly(tmp);
			tempFileModel.setObject(null);
			
			error(getString("common.error.unexpected"));
		}
		
		FeedbackUtils.refreshFeedback(target, getPage());
		target.appendJavaScript(loadingPopup.closeStatement().render());
		
		if (!hasError) {
			ajaxDownload.initiate(target);
		}
		
		onAfterSubmit(target, hasError);
	}
	
	@Override
	protected void onError(AjaxRequestTarget target, Form<?> form) {
		FeedbackUtils.refreshFeedback(target, getPage());
		target.appendJavaScript(loadingPopup.closeStatement().render());
	}
	
	protected void onAfterSubmit(AjaxRequestTarget target, boolean hasError) {
		// Nothing
	}
	
	protected void onEmptyExport(Workbook workbook) {
		error(getString("common.error.export.empty"));
	}
	
	protected abstract Workbook generateWorkbook() throws ServiceException;
	
	@Override
	protected void onDetach() {
		super.onDetach();
		Detachables.detach(tempFileModel, mediaTypeModel);
	}
}
