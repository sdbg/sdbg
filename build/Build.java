import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.exware.nobuto.eclipse.EclipseUpdateSite;
import de.exware.nobuto.eclipse.Repository;
import de.exware.nobuto.eclipse.Unit;
import de.exware.nobuto.java.JavaBuilder;
import de.exware.nobuto.utils.Utilities;
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
        unitGroup.addProperty("org.eclipse.equinox.p2.type.group", "true");
        unitGroup.addCopyright("Copyright 2022, SDBG Developers");
        unitGroup.addLicense("https://www.eclipse.org/legal/epl-v10.html"
            , "https://www.eclipse.org/legal/epl-v10.html"
            , "This project is released under the Eclipse Public License v1.0. For Details look here: https://www.eclipse.org/legal/epl-v10.html");

        for (int i = 0; i < subprojects.size(); i++)
        {
            AbstractSdbgBuild builder = subprojects.get(i);
            String version = builder.getVersion();
            String projectname = builder.getProjectname();
            System.out.println("Build Subproject: " + projectname);
            
            unitGroup.addRequired("org.eclipse.equinox.p2.iu"
                , projectname, version);
            if(builder.getType().equals("feature"))
            {
                repo.addFeatureUnit(projectname, version);
                repo.addArtifact("org.eclipse.update.feature", projectname, version);
            }
            else
            {
                repo.addPluginUnit(projectname, version);
                repo.addArtifact("osgi.bundle", projectname, version);
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
