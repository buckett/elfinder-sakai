package org.sakaiproject.elfinder.sakai.content;

import cn.bluejoe.elfinder.service.FsItem;
import org.sakaiproject.content.api.ContentEntity;
import org.sakaiproject.elfinder.sakai.SakaiFsService;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.exception.SakaiException;

/**
 * Created by buckett on 05/08/15.
 */
public class DropboxFsVolume extends ContentFsVolume {

    public DropboxFsVolume(String siteId, SakaiFsService service) {
        super(siteId, service);
    }

    @Override
    public String getPrefix() {
        return "dropbox";
    }
//
//    @Override
//    public FsItem fromPath(String id) {
//        return new ContentFsItem(this, id);
//    }

    public FsItem getRoot() {
        String id = content.getDropboxCollection(siteId);
        return new ContentFsItem(this, id);
    }


    public String getName(FsItem fsi) {
        String rootId = asId(getRoot());
        String id = asId(fsi);
        if (rootId.equals(id)) {
            // Todo this needs i18n
            return "Dropbox";
        }
        try {
            //ask ContentHostingService for name
            ContentEntity contentEntity;
            if (content.isCollection(id)) {
                contentEntity = content.getCollection(id);
            } else {
                contentEntity = content.getResource(id);
            }
            return contentEntity.getProperties().getProperty(ResourceProperties.PROP_DISPLAY_NAME);
        } catch (SakaiException se) {
            //LOG.warn("Failed to get name for: " + id, se);
        }
        return id;
    }
}
