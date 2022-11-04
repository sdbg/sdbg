package subprojects;
public class SdbgDebugUIBuild extends AbstractSdbgBuild
{
    public SdbgDebugUIBuild()
    {
        super("com.github.sdbg.debug.ui");
    }
    
    @Override
    public void compile() throws Exception
    {
        addSiblingJar("com.github.sdbg.debug.core");
        
        super.compile();
    }

}
