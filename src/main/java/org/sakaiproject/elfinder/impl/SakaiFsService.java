package org.sakaiproject.elfinder.impl;

import cn.bluejoe.elfinder.service.*;

import org.sakaiproject.antivirus.api.VirusFoundException;
import org.sakaiproject.content.api.ContentCollection;
import org.sakaiproject.content.api.ContentEntity;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.content.api.ContentResource;
import org.sakaiproject.content.api.ContentResourceEdit;
import org.sakaiproject.elfinder.sakai.SakaiFsItem;
import org.sakaiproject.elfinder.sakai.SakaiFsVolume;
import org.sakaiproject.exception.OverQuotaException;
import org.sakaiproject.exception.SakaiException;
import org.sakaiproject.exception.ServerOverloadException;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.ToolManager;
import org.apache.commons.codec.binary.Base64;

import java.io.IOException;
import java.io.InputStream;

import static org.sakaiproject.content.api.ContentHostingService.COLLECTION_SITE;

/**
 * Created by buckett on 08/07/15.
 */
public class SakaiFsService implements FsService {

        private ContentHostingService contentHostingService;
        private SiteService siteService;
        private SessionManager sessionManager;
        private ToolManager toolManager;
        
        FsVolume[] _volumes;
        
        String[][] escapes = { { "+", "_P" }, { "-", "_M" }, { "/", "_S" }, { ".", "_D" }, { "=", "_E" } };

        public FsItem fromHash(String hash) throws IOException {
        	if (hash == null || hash.isEmpty()) {
        		return null;
        	}
        	/*String id = getContent().resolveUuid(hash);
            if (id == null) {
            	return null;
            }*/

        	try {
        		//TODO : add cache/map??
				for (String[] pair : escapes)
				{
					hash = hash.replace(pair[1], pair[0]);
				}
				String path = new String(Base64.decodeBase64(hash));
        		ContentEntity contentEntity;
        		if (contentHostingService.isCollection(path)) {
        			contentEntity = contentHostingService.getCollection(path);
        		} else {
        			contentEntity = contentHostingService.getResource(path);
        		}
        		/*String id = contentEntity.getId();
        		String siteId = "";
        		if (id.startsWith(COLLECTION_SITE)) {
        			int nextSlash = id.indexOf('/', COLLECTION_SITE.length());
        			if (nextSlash > 0) {
        				siteId = id.substring(COLLECTION_SITE.length(), nextSlash);
        			}
        		}*/
        		//TODO : getVolumes???? siteId is needed??
        		return new SakaiFsItem(getVolumes()[0], contentEntity.getId());
        	/*} catch (SakaiException se) {
        		throw new IOException("Failed to get file from hash: "+ hash, se);
        	*/} catch (Exception e) {
        		return null;
        	}
        }

        public String getHash(FsItem item) throws IOException {
        	String id = asId(item);

        	String base = new String(Base64.encodeBase64(id.getBytes()));

        	for (String[] pair : escapes)
        	{
        		base = base.replace(pair[0], pair[1]);
        	}
        	//return getContent().getUuid(id);
        	return base;
        }

        public FsSecurityChecker getSecurityChecker() {
                // TODO should be a singleton
                return new SakaiFsSecurityChecker(this);
        }

        public String getVolumeId(FsVolume volume) {
                if (volume instanceof SakaiFsVolume) {
                        return ((SakaiFsVolume)volume).getSiteId();
                } else {
                        throw new IllegalArgumentException("Passed argument isn't SakaiFsVolume");
                }

        }

        public FsVolume[] getVolumes() {
        	//TODO : current site???
        	//TODO : one volume per entity provider(contents, assignments, announcements...)???
        	//TODO : cache??
        	//if(_volumes == null)
        	try {
        		String currentSiteId = "6508bbe2-2016-4b7c-bf0d-3f8a8d9e6042";
        		_volumes = new FsVolume[]{ new SakaiFsVolume(this, currentSiteId)};
    		} catch (Exception e) {
    			e.printStackTrace();
    			return new FsVolume[]{};
    		}
        	return _volumes;
        }

        public FsServiceConfig getServiceConfig() {
                return new SakaiFsServiceConfig();
        }

        public ContentHostingService getContent() {
                return contentHostingService;
        }

        public String getSiteName(String siteId) {
                try {
                        return siteService.getSite(siteId).getTitle();
                } catch (SakaiException se) {
                        return "unknown";
                }
        }

        public String asId(FsItem fsItem) {
                if (fsItem instanceof SakaiFsItem) {
                        return ((SakaiFsItem)fsItem).getId();
                } else {
                        throw new IllegalArgumentException("Passed FsItem must be a SakaiFsItem.");
                }
        }
        
        public String getLocalName(FsItem fsi) {
        	String id = asId(fsi);
        	try {
        		int lastSlash = id.lastIndexOf("/");

        		if(lastSlash < 0) return id;

        		if ((lastSlash+1) == id.length()) {
        			lastSlash = id.lastIndexOf("/", lastSlash-1);
        		}
        		return id.substring(lastSlash+1).replace("/", "");
        	} catch(Exception e) {
        		return id;
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

		public void setSessionManager(SessionManager sessionManager) {
			this.sessionManager = sessionManager;
		}

		public void setToolManager(ToolManager toolManager) {
			this.toolManager = toolManager;
		}
}
