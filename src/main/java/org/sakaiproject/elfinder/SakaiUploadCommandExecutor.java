package org.sakaiproject.elfinder;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

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
			FsItemEx dir = super.findItem((FsService)sfsService, target);
			
			for (final FileItemStream fis : listFiles) {
				String fileName = fis.getName();
				FsItemEx newFile = new FsItemEx(dir, fileName);
				newFile.createFile();
				
				InputStream is = fis.openStream();
				boolean b = sfsService.copyContent(is, newFile.getHash());
				is.close();
				added.add(newFile);
			}
			json.put("added", (Object)this.files2JsonArray(request, (Collection)added));
		}
		else {
			new UploadCommandExecutor().execute(fsService, request, servletContext, json);
		}
	}
}
