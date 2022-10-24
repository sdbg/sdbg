import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.exware.nobuto.Utilities;
import de.exware.nobuto.eclipse.EclipseUpdateSite;
import de.exware.nobuto.eclipse.Repository;
import de.exware.nobuto.eclipse.Unit;
import de.exware.nobuto.java.JavaBuilder;
import subprojects.AbstractSdbgBuild;
import subprojects.Config;
import subprojects.SdbgDebugCoreBuild;
import subprojects.SdbgDebugUIBuild;
import subprojects.SdbgFeatureBuild;
import subprojects.SdbgIntegrationJDTBuild;

public class Build extends JavaBuilder
{
    private List<AbstractSdbgBuild> subprojects = new ArrayList<>();

    public Build()
    {
        subprojects.add(new SdbgDebugCoreBuild());
        subprojects.add(new SdbgDebugUIBuild());
        subprojects.add(new SdbgIntegrationJDTBuild());
        subprojects.add(new SdbgFeatureBuild());
    }

    @Override
    public void dist() throws Exception
    {
        new File(Config.UPDATE_SITE, "plugins").mkdirs();
        new File(Config.UPDATE_SITE, "features").mkdirs();

        Repository repo = EclipseUpdateSite.createRepository();
        Unit unitGroup = repo.addUnit("com.github.sdbg.feature.feature.group", getVersion());
        unitGroup.addProperty("org.eclipse.equinox.p2.name", "Source Map Editor Feature");

        for (int i = 0; i < subprojects.size(); i++)
        {
            AbstractSdbgBuild builder = subprojects.get(i);
            System.out.println("Build Subproject: " + builder.getProjectName());
            
            unitGroup.addRequired("org.eclipse.equinox.p2.iu"
                , builder.getProjectName(), getVersion());
            if(builder.getType().equals("feature"))
            {
                repo.addFeatureUnit(builder.getProjectName(), getVersion());
                repo.addArtifact("org.eclipse.update.feature", builder.getProjectName(), getVersion());
            }
            else
            {
                repo.addPluginUnit(builder.getProjectName(), getVersion());
                repo.addArtifact("osgi.bundle", builder.getProjectName(), getVersion());
            }

            Utilities.delete(Config.TMP + "/make-jar");
            builder.dist();
        }

        Unit unit = repo.addCategoryUnit("com.github.sdbg", getVersion(), "Source Map Debugger",
            "An Eclipse plugin for debugging web applications compiled to JavaScript which have sourcemap support.");
        unit.addRequired("org.eclipse.equinox.p2.iu", "com.github.sdbg.feature.feature.group", getVersion());
        repo.write(new File(Config.UPDATE_SITE));
    }

    public void clean() throws IOException
    {
        System.out.println("Cleaning up");
        Utilities.delete(Config.CLASSES_DIR);
        Utilities.delete(Config.DISTRIBUTION_DIR);
        Utilities.delete(Config.TMP);
        Utilities.delete(Config.UPDATE_SITE);
    }
}
