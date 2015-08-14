package org.sakaiproject.elfinder.sakai.msgcntr;

import org.sakaiproject.elfinder.sakai.FsVolumeFactory;
import org.sakaiproject.elfinder.sakai.SakaiFsService;
import org.sakaiproject.elfinder.sakai.SiteVolume;

/**
 * Created by buckett on 10/08/15.
 */
public class MsgCntrFsVolumeFactory implements FsVolumeFactory {

    private SakaiFsService service;

    public MsgCntrFsVolumeFactory(SakaiFsService service) {
        this.service = service;
    }
    @Override
    public String getPrefix() {
        return "msgcntr";
    }

    @Override
    public SiteVolume getVolume(String siteId) {
        return new MsgCntrFsVolume(service, siteId);
    }

    @Override
    public String getToolId() {
        return "sakai.forums";
    }
}
