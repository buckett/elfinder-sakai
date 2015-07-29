package org.sakaiproject.elfinder.controller.executors;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.sakaiproject.elfinder.impl.SakaiFsService;

import cn.bluejoe.elfinder.controller.executor.AbstractJsonCommandExecutor;
import cn.bluejoe.elfinder.controller.executor.CommandExecutor;
import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.controller.executors.UploadCommandExecutor;
import cn.bluejoe.elfinder.service.FsService;

public class SakaiUploadCommandExecutor extends AbstractJsonCommandExecutor implements CommandExecutor
{
	public void execute(FsService fsService, HttpServletRequest request, ServletContext servletContext, JSONObject json) throws Exception {
		if (fsService instanceof SakaiFsService) {
			SakaiFsService sfsService = (SakaiFsService)fsService;

			List<FileItemStream> listFiles = (List<FileItemStream>)request.getAttribute(FileItemStream.class.getName());
			List<FsItemEx> added = new ArrayList<FsItemEx>();

			String target = request.getParameter("target");
			FsItemEx dir = findItem(sfsService, target);
			for (FileItemStream fis : listFiles) {
				String fileName = fis.getName();
				FsItemEx newFile = new FsItemEx(dir, fileName);
				newFile.createFile();
				
				InputStream is = fis.openStream();
				if(sfsService.copyContent(is, newFile.getHash()))
					added.add(newFile);
				is.close();
			}
			json.put("added", (Object)this.files2JsonArray(request, (Collection)added));
		}
		else {
			new UploadCommandExecutor().execute(fsService, request, servletContext, json);
		}
	}
}
