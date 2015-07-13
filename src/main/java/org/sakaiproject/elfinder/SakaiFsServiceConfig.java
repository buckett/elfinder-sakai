package org.sakaiproject.elfinder;

import cn.bluejoe.elfinder.service.FsServiceConfig;

/**
 * Created by buckett on 10/07/15.
 */
public class SakaiFsServiceConfig implements FsServiceConfig {
    @Override
    public int getTmbWidth() {
        return 80;
    }
}
