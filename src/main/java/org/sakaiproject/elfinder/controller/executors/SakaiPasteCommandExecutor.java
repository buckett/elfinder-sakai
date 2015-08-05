package org.sakaiproject.elfinder.controller.executors;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;
import org.sakaiproject.elfinder.sakai.SakaiFsService;

import cn.bluejoe.elfinder.controller.executor.AbstractJsonCommandExecutor;
import cn.bluejoe.elfinder.controller.executor.CommandExecutor;
import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.service.FsItem;
import cn.bluejoe.elfinder.service.FsService;

public class SakaiPasteCommandExecutor extends AbstractJsonCommandExecutor implements CommandExecutor
{
	@Override
	public void execute(FsService fsService, HttpServletRequest request, ServletContext servletContext, JSONObject json)
			throws Exception
	{
		String[] targets = request.getParameterValues("targets[]");
		String src = request.getParameter("src");
		String dst = request.getParameter("dst");
		boolean cut = "1".equals(request.getParameter("cut"));
			
		List<FsItemEx> added = new ArrayList<FsItemEx>();
		List<String> removed = new ArrayList<String>();

		SakaiFsService sfsService = (SakaiFsService)fsService;
		
		FsItem fdst = sfsService.fromHash(dst);
		FsItemEx fdstex = new FsItemEx(fdst, fsService);

		for (String target : targets)
		{
			FsItem ftgt = sfsService.fromHash(target);
			FsItemEx ftgtex = new FsItemEx(ftgt, fsService);
			
			if(cut) {
				sfsService.getContent().moveIntoFolder(sfsService.asId(ftgt), sfsService.asId(fdst));
				removed.add(target);
			}
			else
				sfsService.getContent().copyIntoFolder(sfsService.asId(ftgt), sfsService.asId(fdst));
			added.add(new FsItemEx(fdstex, ftgt.getVolume().getName(ftgt)));
		}

		json.put("added", files2JsonArray(request, added));
		json.put("removed", removed.toArray());
	}
}
