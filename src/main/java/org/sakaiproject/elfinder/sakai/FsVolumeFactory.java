package org.sakaiproject.elfinder.sakai;

import org.sakaiproject.elfinder.sakai.SiteVolume;

/**
 * This is a factory that tools need to implement which will be called by the service when a new
 * instance of the FsVolume is required for a site. This needs to be high performance as it will be called multiple
 * times in a request.
 */
public interface FsVolumeFactory {

    String getPrefix();

    SiteVolume getVolume(String siteId);

}
