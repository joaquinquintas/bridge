package org.sagebionetworks.bridge.dao;

import org.sagebionetworks.bridge.models.studies.Study;

import com.stormpath.sdk.directory.Directory;

public interface DirectoryDao {

    public String createDirectoryForStudy(Study study);

    public void updateDirectoryForStudy(Study study);
    
    public Directory getDirectoryForStudy(String identifier);

    public void deleteDirectoryForStudy(String identifier);

}
