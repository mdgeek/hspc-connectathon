package org.hspconsortium.cwfdemo.setup;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.carewebframework.common.StrUtil;
import org.springframework.core.io.Resource;

public class DemoSetup {
    
    private static final Log log = LogFactory.getLog(DemoSetup.class);
    
    public DemoSetup(String connectionUrl, String username, String password, Resource sqlResource) throws Exception {
        
        Class.forName("org.h2.Driver");
        
        try (Connection conn = DriverManager.getConnection(connectionUrl, username, password);) {
            List<String> lines = IOUtils.readLines(sqlResource.getInputStream());
            PreparedStatement ps = conn.prepareStatement(StrUtil.fromList(lines));
            ps.execute();
        } catch (Exception e) {
            log.error("Error during demo setup.  This can occur if setup has already been processed.\n\n" + e.getMessage());
        }
    }
    
}
