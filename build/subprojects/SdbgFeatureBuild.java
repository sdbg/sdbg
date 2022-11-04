package subprojects;
import java.io.File;
import java.io.IOException;

import de.exware.nobuto.Utilities;

public class SdbgFeatureBuild extends AbstractSdbgBuild
{
    public SdbgFeatureBuild()
    {
        super("com.github.sdbg.feature");
    }
    
    @Override
    protected void copyEclipseFiles(File classesDir) throws IOException
    {
        super.copyEclipseFiles(classesDir);
        Utilities.replaceInFile(classesDir.getPath() + "/feature.xml", "UTF-8", "version=\"[\\w0-9\\.]+?\"(\\r\\n|\\n)",
            "version=\"" + getVersion() + "\"$1");                            
    }
    
    public String getType()
    {
        return "feature";
    }
}
