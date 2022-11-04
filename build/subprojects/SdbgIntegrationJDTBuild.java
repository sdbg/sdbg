package subprojects;
public class SdbgIntegrationJDTBuild extends AbstractSdbgBuild
{
    public SdbgIntegrationJDTBuild()
    {
        super("com.github.sdbg.integration.jdt");
    }
    
    @Override
    public void compile() throws Exception
    {
        addSiblingJar("com.github.sdbg.debug.core");
        addSiblingJar("com.github.sdbg.debug.ui");
        
        super.compile();
    }

}
