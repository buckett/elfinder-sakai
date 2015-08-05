package org.sakaiproject.elfinder.sakai;

import cn.bluejoe.elfinder.service.FsVolume;

/**
 * This is a FsVolume that's aware of the site it's in.
 */
public interface SiteVolume extends FsVolume {

    /**
     * The SiteId that this FsVolume is for.
     * @return A String site ID.
     */
    String getSiteId();

    /**
     * The prefix that this FsVolume is using. This is needed so the service can generate a hash.
     * @return A String of the prefix this FsVolume uses.
     */
    String getPrefix();

}
