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
            Set<FileObject> loops = fs.searchLoops();
            if(!loops.isEmpty()){
                StringBuilder message = new StringBuilder("[Error] There are loop in files: \n");
                for (FileObject file : loops) {
                    message.append(fs.getPath(file.file)).append("\n");
                }
                throw new Exception(message.toString());
            }
            Vector<FileObject> orderedByDependency = fs.orderByDependency();
            fs.buildFiles(orderedByDependency);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

    }
}