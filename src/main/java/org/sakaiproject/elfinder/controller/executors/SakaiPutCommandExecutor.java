package org.sakaiproject.elfinder.controller.executors;

import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.sakaiproject.elfinder.impl.SakaiFsService;

import cn.bluejoe.elfinder.controller.executor.AbstractJsonCommandExecutor;
import cn.bluejoe.elfinder.controller.executor.CommandExecutor;
import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.service.FsService;

public class SakaiPutCommandExecutor extends AbstractJsonCommandExecutor implements CommandExecutor
{
	@Override
	public void execute(FsService fsService, HttpServletRequest request, ServletContext servletContext, JSONObject json)
			throws Exception
	{
		SakaiFsService sfsService = (SakaiFsService)fsService;
		
		String target = request.getParameter("target");

		FsItemEx fsi = super.findItem(fsService, target);
		
		if(((SakaiFsService)fsService).copyContent(request.getParameter("content"), target))
			json.put("changed", new Object[] { super.getFsItemInfo(request, fsi) });
	}
}
