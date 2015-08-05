package org.sakaiproject.elfinder.sakai.content;

import org.sakaiproject.elfinder.sakai.FsVolumeFactory;
import org.sakaiproject.elfinder.sakai.SakaiFsService;
import org.sakaiproject.elfinder.sakai.SiteVolume;

/**
 * This is the creator of ContentHosting FsVolumes.
 */
public class DropboxFsVolumeFactory implements FsVolumeFactory {

    private SakaiFsService service;

    public DropboxFsVolumeFactory(SakaiFsService service) {

        this.service = service;
    }

    @Override
    public String getPrefix() {
        return "dropbox";
    }

    @Override
    public SiteVolume getVolume(String siteId) {
        return new DropboxFsVolume(siteId, service);
    }
}
