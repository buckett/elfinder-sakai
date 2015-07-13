package org.sakaiproject.elfinder;

import cn.bluejoe.elfinder.service.*;
import org.sakaiproject.content.api.ContentCollection;
import org.sakaiproject.content.api.ContentEntity;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.content.api.ContentResource;
import org.sakaiproject.exception.SakaiException;
import org.sakaiproject.site.api.SiteService;

import java.io.IOException;

import static org.sakaiproject.content.api.ContentHostingService.COLLECTION_SITE;

/**
 * Created by buckett on 08/07/15.
 */
public class SakaiFsService implements FsService {

        private ContentHostingService contentHostingService;
        private SiteService siteService;

        public FsItem fromHash(String hash) throws IOException {
                if (hash == null || hash.isEmpty()) {
                       return null;
                }
                String id = getContent().resolveUuid(hash);
                if (id == null) {
                        return null;
                }
                String siteId = "";
                if (id.startsWith(COLLECTION_SITE)) {
                        int nextSlash = id.indexOf('/', COLLECTION_SITE.length());
                        if (nextSlash > 0) {
                                 siteId = id.substring(COLLECTION_SITE.length(), nextSlash);
                        }
                }
                // Todo need to get Site ID from path.
                try {
                        ContentEntity contentEntity;
                        if (contentHostingService.isCollection(id)) {
                                contentEntity = contentHostingService.getCollection(id);
                        } else {
                                contentEntity = contentHostingService.getResource(id);
                        }
                        return new SakaiFsItem(new SakaiFsVolume(this, siteId), contentEntity.getId());
                } catch (SakaiException se) {
                        throw new IOException("Failed to get file from hash: "+ id, se);
                }
        }

        public String getHash(FsItem item) throws IOException {
                String id = asId(item);
                return getContent().getUuid(id);
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
                return new FsVolume[]{ new SakaiFsVolume(this, "080ede53-4167-4efd-bff2-6f406907a78f")};
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

        public void setSiteService(SiteService siteService) {
                this.siteService = siteService;
        }

        public void setContentHostingService(ContentHostingService contentHostingService) {
                this.contentHostingService = contentHostingService;
        }
}
