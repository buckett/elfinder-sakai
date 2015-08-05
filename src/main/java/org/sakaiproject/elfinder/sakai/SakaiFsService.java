package org.sakaiproject.elfinder.sakai;

import cn.bluejoe.elfinder.service.*;
import org.apache.commons.codec.binary.Base64;
import org.sakaiproject.antivirus.api.VirusFoundException;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.content.api.ContentResourceEdit;
import org.sakaiproject.elfinder.impl.SakaiFsSecurityChecker;
import org.sakaiproject.elfinder.impl.SakaiFsServiceConfig;
import org.sakaiproject.elfinder.sakai.content.ContentFsItem;
import org.sakaiproject.elfinder.sakai.content.ContentFsVolume;
import org.sakaiproject.elfinder.sakai.content.ContentFsVolumeFactory;
import org.sakaiproject.elfinder.sakai.content.DropboxFsVolumeFactory;
import org.sakaiproject.elfinder.sakai.site.SiteFsItem;
import org.sakaiproject.elfinder.sakai.site.SiteFsVolume;
import org.sakaiproject.exception.OverQuotaException;
import org.sakaiproject.exception.ServerOverloadException;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.sakaiproject.site.api.SiteService.SelectionType.ACCESS;

/**
 *
 * Volume layout:
 * /site1
 * /site1/content
 * /site1/forums
 * /site2
 * /site2/content
 * /site2/forums
 *
 * Then within each volume there will be files. The one that needs to be done really carefully is
 */
public class SakaiFsService implements FsService {

        private ContentHostingService contentHostingService;
        private SiteService siteService;

	Map<String, FsVolumeFactory> toolVolume;

	public Map<String, FsVolumeFactory> getToolVolume() {
		return toolVolume;
	}

	public SakaiFsService() {
		toolVolume = new HashMap<>();
		toolVolume.put("content", new ContentFsVolumeFactory(this));
		toolVolume.put("dropbox", new DropboxFsVolumeFactory(this));
	}

        String[][] escapes = { { "+", "_P" }, { "-", "_M" }, { "/", "_S" }, { ".", "_D" }, { "=", "_E" } };

        public FsItem fromHash(String hash) throws IOException {
        	if (hash == null || hash.isEmpty()) {
        		return null;
        	}
			for (String[] pair : escapes)
			{
				hash = hash.replace(pair[1], pair[0]);
			}
			String path = new String(Base64.decodeBase64(hash));

			// Work out what we're dealing with.
			String[] parts = path.split("/");
			String siteId = null;
			String tool = null;
			String volumePath = null;
			if (parts.length > 1) {
				siteId = parts[1];
				if (parts.length > 2) {
					tool = parts[2];
					if (parts.length > 3) {
						StringBuilder builder = new StringBuilder();
						for (int i = 3; i < parts.length; i++) {
							builder.append("/");
							builder.append(parts[i]);
						}
						// This gets lost in the split
						if (path.endsWith("/")) {
							builder.append("/");
						}
						volumePath = builder.toString();
					}
				}
			}

			FsItem fsItem = null;
			if (tool != null) {
				FsVolumeFactory factory = toolVolume.get(tool);
				if (factory != null) {
					FsVolume volume = factory.getVolume(siteId);
					fsItem = volume.fromPath(volumePath);
					// Todo return this item.
				}
			} else if (siteId != null) {
				fsItem = new SiteFsVolume(siteId, this).getRoot();
			}
			return fsItem;
		}

        public String getHash(FsItem item) throws IOException {
			// Need to get prefix.
			StringBuilder path = new StringBuilder();
			FsVolume volume = item.getVolume();
			if (volume instanceof SiteFsVolume) {
				path.append("/").append(((SiteFsVolume) volume).getSiteId());
			} else if (volume instanceof SiteVolume) {
				path.append("/").append(((SiteVolume)volume).getSiteId());
				// Need prefix but don't want volumes to be able to screw it up.
				path.append("/").append(((SiteVolume)volume).getPrefix());
			} else {
				throw new IllegalArgumentException("Expected different type of FsItem: "+ volume.getClass());
			}
			String volumePath = volume.getPath(item);
			// We have to have a separator but don't want multiple ones.
			if (volumePath != null ) {
				if (!volumePath.startsWith("/")) {
					path.append("/");
				}
				path.append(volumePath);
			}

        	String base = new String(Base64.encodeBase64(path.toString().getBytes()));

        	for (String[] pair : escapes)
        	{
        		base = base.replace(pair[0], pair[1]);
        	}
        	return base;
        }

        public FsSecurityChecker getSecurityChecker() {
                // TODO should be a singleton
                return new SakaiFsSecurityChecker(this);
        }

        public String getVolumeId(FsVolume volume) {
                if (volume instanceof ContentFsVolume) {
					// This should be the siteID plus /content
					// but I wouldn't expect to ever see this as
					return ((ContentFsVolume) volume).getSiteId();
				} else if (volume instanceof SiteFsVolume) {
						// Will return the site ID
						return ((SiteFsVolume)volume).getSiteId();
                } else {
                        throw new IllegalArgumentException("Passed argument isn't SakaiFsVolume");
                }

        }

        public FsVolume[] getVolumes() {
			List<Site> sites  = siteService.getSites(ACCESS, null, null, null, null, null);
			List<FsVolume> volumes = new ArrayList<>(sites.size());
			for (Site site: sites) {
                String currentSiteId = site.getId();
                volumes.add(new SiteFsVolume(currentSiteId, this));
			}
        	return volumes.toArray(new FsVolume[0]);
        }

        public FsServiceConfig getServiceConfig() {
                return new SakaiFsServiceConfig();
        }

        public ContentHostingService getContent() {
                return contentHostingService;
        }


        public String asId(FsItem fsItem) {
                if (fsItem instanceof ContentFsItem) {
					return ((ContentFsItem) fsItem).getId();
				} else if (fsItem instanceof SiteFsItem) {
					return ((SiteFsItem)fsItem).getId();
                } else {
                        throw new IllegalArgumentException("Passed FsItem must be a SakaiFsItem.");
                }
        }
        public Boolean copyContent(InputStream is, String hash) {
        	try {
        		String id = asId(fromHash(hash));
        		ContentResourceEdit resource = getContent().editResource(id);
        		resource.setContent(is);
        		getContent().commitResource(resource);
        		return true;
        	} catch (OverQuotaException | ServerOverloadException | VirusFoundException e) {
        		e.printStackTrace();

        	} catch (Exception e) {
        		e.printStackTrace();
        	}
        	return false;
        }
        
        public Boolean copyContent(String content, String hash) {
        	try {
        		String id = asId(fromHash(hash));
        		ContentResourceEdit resource = getContent().editResource(id);
        		resource.setContent(content.getBytes("UTF-8"));
        		getContent().commitResource(resource);
        		return true;
        	} catch (OverQuotaException | ServerOverloadException | VirusFoundException e) {
        		e.printStackTrace();

        	} catch (Exception e) {
        		e.printStackTrace();
        	}
        	return false;
        }

        public void setSiteService(SiteService siteService) {
                this.siteService = siteService;
        }

        public void setContentHostingService(ContentHostingService contentHostingService) {
                this.contentHostingService = contentHostingService;
        }

}
