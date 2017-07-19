package foo.changes;

import foo.model.ProjectNode;
import foo.repository.ChangeRepository;
import foo.utils.NodeUtils;
import foo.workspace.Workspace;
import org.junit.Test;

import java.net.URL;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

public class ChangeRepositoryTest {
    private Workspace workspace = new Workspace();

    @Test
    public void fetch() throws Exception {
        URL url = ChangeRepositoryTest.class.getResource("/changes.yaml");
        List<Change> changes = ChangeRepository.fetch(url);

        workspace.apply(changes);

        Optional<ProjectNode> myProjectNodeOpt = workspace.getProjects().stream()
                .filter(n -> n.name().equals("My Project")).findFirst();
        assertTrue(myProjectNodeOpt.isPresent());
        assertEquals(
            "project My Project\n" +
                "\tpackage mypackage\n" +
                "\t\tdef foo [x y]\n" +
                "\t\t\t- [* [x y] 100]",
            NodeUtils.toString(myProjectNodeOpt.get()));
    }

}