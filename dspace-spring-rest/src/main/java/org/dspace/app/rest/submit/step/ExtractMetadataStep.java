/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.step;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.rest.model.ErrorRest;
import org.dspace.app.rest.repository.WorkspaceItemRestRepository;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.app.rest.submit.UploadableStep;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.services.model.Request;
import org.dspace.submit.AbstractProcessingStep;
import org.dspace.submit.extraction.MetadataExtractor;
import org.springframework.web.multipart.MultipartFile;

import gr.ekt.bte.core.Record;
import gr.ekt.bte.core.RecordSet;
import gr.ekt.bte.core.Value;
import gr.ekt.bte.dataloader.FileDataLoader;

/**
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 *
 */
public class ExtractMetadataStep extends AbstractProcessingStep implements UploadableStep {

	private static final Logger log = Logger.getLogger(ExtractMetadataStep.class);

	@Override
	public ErrorRest upload(Context context, SubmissionService submissionService, SubmissionStepConfig stepConfig,
			WorkspaceItem wsi, MultipartFile multipartFile, String extraField) throws IOException {

		Item item = wsi.getItem();
		try {
			List<MetadataExtractor> extractors = DSpaceServicesFactory.getInstance().getServiceManager()
					.getServicesByType(MetadataExtractor.class);
			File file = null;
			for (MetadataExtractor extractor : extractors) {
				FileDataLoader dataLoader = extractor.getDataLoader();
				RecordSet recordSet = null;
				if (extractor.getExtensions().contains(FilenameUtils.getExtension(multipartFile.getOriginalFilename()))) {

					if (file == null) {
						file = getFile(stepConfig, multipartFile);
					}

					FileDataLoader fdl = (FileDataLoader) dataLoader;
					fdl.setFilename(file.getAbsolutePath());
					
					
					recordSet = convertFields(dataLoader.getRecords(), bteBatchImportService.getOutputMap());
					 			
					enrichItem(context, recordSet, item);

				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			ErrorRest result = new ErrorRest();
			result.setMessage(e.getMessage());
			result.getPaths().add("/" + WorkspaceItemRestRepository.OPERATION_PATH_SECTIONS + "/" + stepConfig.getId());
			return result;
		}
		return null;
	}

	private void enrichItem(Context context, RecordSet rset, Item item) throws SQLException {
		for (Record record : rset.getRecords()) {
			for (String field : record.getFields()) {
				String[] tfield = Utils.tokenize(field);
				List<MetadataValue> mdvs = itemService.getMetadata(item, tfield[0], tfield[1], tfield[2], Item.ANY);
				if (mdvs == null || mdvs.isEmpty()) {
					for (Value value : record.getValues(field)) {
						itemService.addMetadata(context, item, tfield[0], tfield[1], tfield[2], null,
								value.getAsString());
					}
				} else {
					external: for (Value value : record.getValues(field)) {
						boolean found = false;
						for (MetadataValue mdv : mdvs) {
							if (mdv.getValue().equals(value.getAsString())) {
								found = true;
								continue external;
							}
						}
						if (!found) {
							itemService.addMetadata(context, item, tfield[0], tfield[1], tfield[2], null,
									value.getAsString());
						}
					}
				}
			}
		}

	}

	private File getFile(SubmissionStepConfig stepConfig, MultipartFile multipartFile)
			throws IOException, FileNotFoundException {
		// TODO after change item-submission into
		String tempDir = (ConfigurationManager.getProperty("upload.temp.dir") != null)
				? ConfigurationManager.getProperty("upload.temp.dir") : System.getProperty("java.io.tmpdir");
		File uploadDir = new File(tempDir);
		if (!uploadDir.exists()) {
			if (!uploadDir.mkdir()) {
				uploadDir = null;
			}
		}
		File file = File.createTempFile("submissionlookup-loader-" + stepConfig.getId(), ".temp", uploadDir);
		InputStream io = new BufferedInputStream(multipartFile.getInputStream());
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
		Utils.bufferedCopy(io, out);
		return file;
	}

	@Override
	public void doProcessing(Context context, Request req, InProgressSubmission wsi) {
		// TODO Auto-generated method stub

	}

	@Override
	public void doPostProcessing(Context context, Request obj, InProgressSubmission wsi) {
		// TODO Auto-generated method stub

	}

	public RecordSet convertFields(RecordSet recordSet, Map<String, String> fieldMap) {
		RecordSet result = new RecordSet();
		for (Record publication : recordSet.getRecords()) {
			for (String fieldName : fieldMap.keySet()) {
				String md = null;
				if (fieldMap != null) {
					md = fieldMap.get(fieldName);
				}

				if (StringUtils.isBlank(md)) {
					continue;
				} else {
					md = md.trim();
				}

				if (publication.isMutable()) {
					List<Value> values = publication.getValues(md);
					publication.makeMutable().removeField(md);
					publication.makeMutable().addField(fieldName, values);
				}
			}

			result.addRecord(publication);
		}
		return result;
	}
}
