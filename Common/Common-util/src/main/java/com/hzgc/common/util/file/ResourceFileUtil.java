package com.hzgc.common.util.file;

import com.hzgc.common.util.empty.IsEmpty;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

public class ResourceFileUtil {

    private static Logger LOG = Logger.getLogger(ResourceFileUtil.class);

    public static File loadResourceFile(String resourceName) {
        if (IsEmpty.strIsRight(resourceName)) {
            URL url = ClassLoader.getSystemResource(resourceName);
            if (url != null) {
                File file = new File(url.getPath());
                LOG.info("Load resource file:" + url.getPath() + " successful!");
                return file;
            } else {
                LOG.error("Resource file:" +
                        ClassLoader.getSystemResource("") + resourceName + " is not exist!");
                System.exit(1);
            }
        } else {
            LOG.error("The file name is not vaild!");
        }
        return new File("");
    }

    public static InputStream loadResourceInputStream(String resourceName) {
        if (IsEmpty.strIsRight(resourceName)) {
            InputStream resourceStream = ClassLoader.getSystemClassLoader().getResourceAsStream(resourceName);
            if (resourceStream != null) {
                LOG.info("Load resource file:" + ClassLoader.getSystemResource(resourceName).getPath() + " successful!");
                return resourceStream;
            } else {
                LOG.error("Resource file:" +
                        ClassLoader.getSystemResource("") + resourceName + " is not exist!");
                System.exit(1);
            }
        } else {
            LOG.error("The file name is not vaild!");
        }
        return null;
    }
}
