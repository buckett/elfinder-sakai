package org.sakaiproject.elfinder.sakai;

import cn.bluejoe.elfinder.service.FsItem;
import cn.bluejoe.elfinder.service.FsVolume;

/**
 * Created by buckett on 08/07/15.
 */
public class SakaiFsItem implements FsItem {

    private final FsVolume fsVolume;
    private String id;

    public SakaiFsItem(FsVolume fsVolume, String id) {
        this.fsVolume = fsVolume;
        this.id = id;
    }

    @Override
    public FsVolume getVolume() {
        return fsVolume;
    }

    public String getId() {
        return id;
    }

	public void setId(String id) {
		this.id = id;
	}
}
