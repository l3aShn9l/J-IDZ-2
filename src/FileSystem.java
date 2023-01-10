import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class FileSystem {
    File directory;
    Vector<FileObject> orderedByNameFiles;
    Hashtable<FileObject, Hashtable<FileObject, Boolean>> dependencyMatrix;

    public FileSystem(String directoryPath) throws Exception {
        directory = new File(directoryPath);
        orderedByNameFiles = new Vector<>();
        dependencyMatrix = new Hashtable<>();
        if (!directory.isDirectory()) {
            throw new Exception("Wrong path");
        }
        System.out.println("Getting files...");
        getFiles(directoryPath);
        orderedByNameFiles.sort(Comparator.comparing(file -> file.file.getName()));
        for (FileObject file : orderedByNameFiles) {
            Hashtable<FileObject, Boolean> dependencies = new Hashtable<>();
            for (FileObject another : orderedByNameFiles) {
                dependencies.put(another, false);
            }
            dependencyMatrix.put(file, dependencies);
        }
        updateDependencies();
    }

    private void getFiles(String directoryPath) {
        File directory = new File(directoryPath);
        for (File o : Objects.requireNonNull(directory.listFiles())) {
            if (o.isFile()) {
                orderedByNameFiles.add(new FileObject(o));
            } else if (o.isDirectory()) {
                getFiles(o.getAbsolutePath());
            }
        }
    }

    public String getPath(File file) throws Exception {
        if (!file.getAbsolutePath().contains(directory.getName())) {
            throw new Exception("Error 1");
        }
        File parent = file.getParentFile();
        StringBuilder path = new StringBuilder(file.getName().substring(0, file.getName().lastIndexOf('.')));
        while (!parent.getAbsolutePath().equals(directory.getAbsolutePath())) {
            path.insert(0, parent.getName() + "/");
            parent = parent.getParentFile();
        }
        return path.toString();
    }

    private void updateDependencies() throws Exception {
        System.out.println("Updating dependencies...");
        /*
        System.out.println("Dependency matrix: ");
        */
        for (FileObject file : orderedByNameFiles) {
            for (FileObject another : orderedByNameFiles) {
                if (file.text.contains("require ‘" + getPath(another.file) + "’")) {
                    dependencyMatrix.get(file).replace(another, true);
                    /*
                    System.out.print("1 ");
                    */
                } else {
                    /*
                    System.out.print("0 ");
                    */
                }
            }

            /*
            System.out.print("\n");
            */
        }
    }

    private Vector<FileObject> getLeavesAndRoots(Hashtable<FileObject, Hashtable<FileObject, Boolean>> matrix) {
        Vector<FileObject> deletable = new Vector<>();
        Set<FileObject> keySetX = matrix.keySet();
        for (FileObject fileX : keySetX) {
            Hashtable<FileObject, Boolean> v = matrix.get(fileX);
            boolean leaf = true;
            Set<FileObject> keySetY = v.keySet();
            for (FileObject fileY : keySetY) {
                leaf = leaf && !v.get(fileY);
            }
            if (leaf) {
                deletable.add(fileX);
            }
        }
        for (FileObject file : keySetX) {
            boolean root = true;
            for (FileObject fileX : keySetX) {
                Hashtable<FileObject, Boolean> v = matrix.get(fileX);
                root = root && !v.get(file);
            }
            if (root) {
                deletable.add(file);
            }
        }
        return deletable;
    }

    private Vector<FileObject> getLeaves(Hashtable<FileObject, Hashtable<FileObject, Boolean>> matrix) {
        Vector<FileObject> deletable = new Vector<>();
        Set<FileObject> keySetX = matrix.keySet();
        for (FileObject fileX : keySetX) {
            Hashtable<FileObject, Boolean> v = matrix.get(fileX);
            boolean leaf = true;
            Set<FileObject> keySetY = v.keySet();
            for (FileObject fileY : keySetY) {
                leaf = leaf && !v.get(fileY);
            }
            if (leaf) {
                deletable.add(fileX);
            }
        }
        return deletable;
    }

    private Hashtable<FileObject, Hashtable<FileObject, Boolean>> getUnorderedDependencyMatrixCopy() {
        Hashtable<FileObject, Hashtable<FileObject, Boolean>> copy = new Hashtable<>();
        Set<FileObject> kSX = dependencyMatrix.keySet();
        for (FileObject elX : kSX) {
            Set<FileObject> kSY = dependencyMatrix.get(elX).keySet();
            Hashtable<FileObject, Boolean> v = new Hashtable<>();
            for (FileObject elY : kSY) {
                v.put(elY, dependencyMatrix.get(elX).get(elY));
            }
            copy.put(elX, v);
        }
        return copy;
    }

    private void printMatrix(Hashtable<FileObject, Hashtable<FileObject, Boolean>> matrix) {
        Set<FileObject> kSX = matrix.keySet();
        for (FileObject elX : kSX) {
            Set<FileObject> kSY = matrix.get(elX).keySet();
            for (FileObject elY : kSY) {
                if (matrix.get(elX).get(elY)) {
                    System.out.print("1 ");
                } else {
                    System.out.print("0 ");
                }
            }
            System.out.print("\n");
        }
        System.out.print("\n");
    }

    public Set<FileObject> searchLoops() throws Exception {
        System.out.println("Searching loops...");
        Hashtable<FileObject, Hashtable<FileObject, Boolean>> copy = getUnorderedDependencyMatrixCopy();
        /*
        System.out.println("Unordered dependency matrix:");
        printMatrix(copy);
        */
        Vector<FileObject> deletable = getLeavesAndRoots(copy);
        while (!deletable.isEmpty()) {
            /*
            System.out.println("Deletable: ");
            for (FileObject el:deletable) {
                System.out.println(getPath(el.file));
            }
            System.out.print("\n");
            */
            for (FileObject file : deletable) {
                copy.remove(file);
                Set<FileObject> keySetX = copy.keySet();
                for (FileObject fileX : keySetX) {
                    copy.get(fileX).remove(file);
                }
                /*
                printMatrix(copy);
                */
            }
            deletable = getLeavesAndRoots(copy);
        }
        return copy.keySet();
    }

    public Vector<FileObject> orderByDependency() throws Exception {
        System.out.println("Ordering files by dependency...");
        Hashtable<FileObject, Hashtable<FileObject, Boolean>> copy = getUnorderedDependencyMatrixCopy();
        /*
        System.out.println("Unordered dependency matrix:");
        printMatrix(copy);
        */
        Vector<FileObject> deletable = getLeaves(copy);
        Vector<FileObject> orderedByDependency = new Vector<>();
        while (!deletable.isEmpty()) {
            /*
            System.out.println("Deletable: ");
            for (FileObject el:deletable) {
                System.out.println(getPath(el.file));
            }
            System.out.print("\n");
            */
            for (FileObject file : deletable) {
                orderedByDependency.add(file);
                copy.remove(file);
                Set<FileObject> keySetX = copy.keySet();
                for (FileObject fileX : keySetX) {
                    copy.get(fileX).remove(file);
                }
                /*
                printMatrix(copy);
                */
            }
            deletable = getLeaves(copy);
        }
        if (copy.size() != 0) {
            throw new Exception("Error 2");
        }
        /*
        System.out.println("Ordered by dependency list: ");
        for (FileObject file:orderedByDependency) {
            System.out.println(file.file.getAbsolutePath());
        }
        */
        return orderedByDependency;
    }

    public void buildFiles(Vector<FileObject> orderedByDependency) throws Exception {
        System.out.println("Building files...");
        for (FileObject file : orderedByDependency) {
            for (FileObject another : orderedByNameFiles) {
                if (file.text.contains("require ‘" + getPath(another.file) + "’")) {
                    file.text = file.text.replaceAll("require ‘" + getPath(another.file) + "’", another.text);
                }
            }
            Pattern pattern = Pattern.compile("require ‘.+?’");
            Matcher matcher = pattern.matcher(file.text);
            if (matcher.find()) {
                StringBuilder message = new StringBuilder("[Error] File " + getPath(file.file) + " have dependency with nonexistent file: \n");
                message.append(file.text.substring(matcher.start(), matcher.end())).append("\n");
                while (matcher.find()) {
                    message.append(file.text.substring(matcher.start(), matcher.end())).append("\n");
                }
                throw new Exception(message.toString());
            }
            try (FileWriter writer = new FileWriter(file.file, false)) {
                writer.write(file.text);
                writer.flush();
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }
}

class FileObject {
    FileObject(File file) {
        this.file = file;
        try {
            this.text = Files.readString(Path.of(file.getAbsolutePath()));
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    String text;
    File file;
}

