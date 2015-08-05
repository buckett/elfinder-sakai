package org.sakaiproject.elfinder.sakai.content;

import org.sakaiproject.elfinder.sakai.FsVolumeFactory;
import org.sakaiproject.elfinder.sakai.SakaiFsService;
import org.sakaiproject.elfinder.sakai.SiteVolume;

/**
 * This is the creator of ContentHosting FsVolumes.
 */
public class ContentFsVolumeFactory implements FsVolumeFactory {

    private SakaiFsService service;

    public ContentFsVolumeFactory(SakaiFsService service) {

        this.service = service;
    }

    @Override
    public String getPrefix() {
        return "content";
    }

    @Override
    public SiteVolume getVolume(String siteId) {
        return new ContentFsVolume(siteId, service);
    }
}
