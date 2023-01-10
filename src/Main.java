import java.util.Scanner;
import java.util.Set;
import java.util.Vector;

public class Main {
    public static void main(String[] args) {
        String path;
        Scanner in = new Scanner(System.in);
        path = in.nextLine();
        try {
            FileSystem fs = new FileSystem(path);
            fs.searchLoops();
            Vector<FileObject> orderedByDependency = fs.orderByDependency();
            fs.buildFiles(orderedByDependency);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

    }
}