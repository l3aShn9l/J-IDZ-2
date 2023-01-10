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
            throw new Exception("Incorrect path");
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

    /**
     * С помощью функции мы получаем все файлы в корневой директории и ее поддиректориях.
     * @param directoryPath Путь до корневой директории.
     */
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

    /**
     * Функция возвращает нам путь файла в необходимом для задания формате.
     * @param file Файл, путь которого мы хотим привести в необходимый нам формат.
     * @return Возвращает путь в необходимом формате.
     * @throws Exception Выбрасывает сообщение об ошибке, если не удается совершить file.getName().
     */
    public String getPath(File file) throws Exception {
        if (!file.getAbsolutePath().contains(directory.getName())) {
            throw new Exception("Error");
        }
        File parent = file.getParentFile();
        StringBuilder path = new StringBuilder(file.getName().substring(0, file.getName().lastIndexOf('.')));
        while (!parent.getAbsolutePath().equals(directory.getAbsolutePath())) {
            path.insert(0, parent.getName() + "/");
            parent = parent.getParentFile();
        }
        return path.toString();
    }

    /**
     * Обновляет матрицу зависимостей, а также проверяет все require. Многострочнымми комментариями закомментирован вывод получившейся матрицы зависимостей.
     * @throws Exception Выбрасывает сообщение об ошибке, если обнаружены некорректные require или же не удается совершить getPath(file.file).
     */
    private void updateDependencies() throws Exception {
        System.out.println("Updating dependencies...");
        StringBuilder message = new StringBuilder("[Error] Incorrect dependencies\n");
        for (FileObject file : orderedByNameFiles) {
            boolean error = false;
            Pattern pattern = Pattern.compile("require ‘.+?’");
            Matcher matcher = pattern.matcher(file.text);
            StringBuilder badDependencies = new StringBuilder("File " + getPath(file.file) + " have dependency with nonexistent files: \n");
            while (matcher.find()) {
                String result = file.text.substring(matcher.start(), matcher.end());
                boolean invalid = true;
                for (FileObject another : orderedByNameFiles) {
                    if (result.equals("require ‘" + getPath(another.file) + "’")) {
                        dependencyMatrix.get(file).replace(another, true);
                        invalid = false;
                    }
                }
                if(invalid){
                    error = true;
                    badDependencies.append(result).append("\n");
                }
            }
            if (error){
                message.append(badDependencies);
            }
        }
        if(!message.toString().equals("[Error] Incorrect dependencies\n")){
            throw new Exception(message.toString());
        }
        //Вывод полученной матрицы зависимостей
        /*
        System.out.println("Dependency matrix: ");
        printOrderedMatrix(dependencyMatrix);
        */
    }

    /**
     * Функция возвращает файлы, которые являются листовыми или корневыми в переданной матрице зависимостей.
     * @param matrix Обрабатываемая матрица зависимостей.
     * @return Возвращает вектор файлов, которые являются листовыми (не имеют зависимостей) или корневыми (не имеют зависимых файлов).
     */
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

    /**
     * Функция возвращает файлы, которые являются листовыми в переданной матрице зависимостей.
     * @param matrix Обрабатываемая матрица зависимостей.
     * @return Возвращает вектор файлов, которые являются листовыми (не имеют зависимостей).
     */
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

    /*
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
    */

    /**
     * Функция отвечает за вывод матрицы зависимостей в отсортированном по имени виде, в отличии от своего закомментированого аналога.
     * @param matrix Матрица передаваемая для вывода.
     */
    private void printOrderedMatrix(Hashtable<FileObject, Hashtable<FileObject, Boolean>> matrix) {
        Set<FileObject> kSX = matrix.keySet();
        for (FileObject elX : orderedByNameFiles) {
            if(kSX.contains(elX)) {
                Set<FileObject> kSY = matrix.get(elX).keySet();
                for (FileObject elY : orderedByNameFiles) {
                    if(kSY.contains(elY)) {
                        if (matrix.get(elX).get(elY)) {
                            System.out.print("1 ");
                        } else {
                            System.out.print("0 ");
                        }
                    }
                }
                System.out.print("\n");
            }
        }
        System.out.print("\n");
    }

    /**
     * Функция отвечает за получение копии главной матрицы зависимостей в отсортированном по имени виде, в отличии от своего закомментированого аналога.
     * @return Возвращает копию матрицы зависимостей в отсортированном по имени виде.
     */
    private Hashtable<FileObject, Hashtable<FileObject, Boolean>> getOrderedDependencyMatrixCopy() {
        Hashtable<FileObject, Hashtable<FileObject, Boolean>> copy = new Hashtable<>();
        Set<FileObject> kSX = dependencyMatrix.keySet();
        for (FileObject elX : orderedByNameFiles) {
            if (kSX.contains(elX)) {
                Set<FileObject> kSY = dependencyMatrix.get(elX).keySet();
                Hashtable<FileObject, Boolean> v = new Hashtable<>();
                for (FileObject elY : orderedByNameFiles) {
                    if(kSY.contains(elY)) {
                        v.put(elY, dependencyMatrix.get(elX).get(elY));
                    }
                }
                copy.put(elX, v);
            }
        }
        return copy;
    }
    /*
    private void printUnorderedMatrix(Hashtable<FileObject, Hashtable<FileObject, Boolean>> matrix) {
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
    */

    /**
     * Проверка исходных данных на зацикливания. За многострочными комментариями скрыт код для отладки, который по шагам иллюстрирует процесс поиска зацикливаний.
     * @throws Exception Выбрасывает сообщение об ошибке, если обнаружены зацикливания в исходных данных.
     */
    public void searchLoops() throws Exception {
        System.out.println("Searching loops...");
        Hashtable<FileObject, Hashtable<FileObject, Boolean>> copy = getOrderedDependencyMatrixCopy();
        /*
        System.out.println("Dependency matrix:");
        printOrderedMatrix(copy);
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
                printOrderedMatrix(copy);
                */
            }
            deletable = getLeavesAndRoots(copy);
        }
        if(!copy.keySet().isEmpty()){
            StringBuilder message = new StringBuilder("[Error] There are loop in files: \n");
            for (FileObject file : copy.keySet()) {
                message.append(getPath(file.file)).append("\n");
            }
            throw new Exception(message.toString());
        }
    }

    /**
     * Функция отвечает за получение упорядоченного по уровню зависимости списка файлов. За многострочными комментариями скрыт код для отладки, который по шагам иллюстрирует процесс поиска зацикливаний, а также дает возможность посмотреть на полученный список.
     * @return Возвращает упорядоченный по уровню зависимости список файлов.
     * @throws Exception Выбрасывает сообщение об ошибке, если не доработает до конца (больше для отладки).
     */
    public Vector<FileObject> orderByDependency() throws Exception {
        System.out.println("Ordering files by dependency...");
        Hashtable<FileObject, Hashtable<FileObject, Boolean>> copy = getOrderedDependencyMatrixCopy();
        /*
        System.out.println("Dependency matrix:");
        printOrderedMatrix(copy);
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
                printOrderedMatrix(copy);
                */
            }
            deletable = getLeaves(copy);
        }
        if (copy.size() != 0) {
            throw new Exception("Error");
        }
        //Ниже вывод списка файлов упорядоченного по уровню зависимости
        /*
        System.out.println("Ordered by dependency list: ");
        for (FileObject file:orderedByDependency) {
            System.out.println(file.file.getAbsolutePath());
        }
        */
        return orderedByDependency;
    }

    /**
     * Функция отвечает за конечную сборку всех файлов
     * @param orderedByDependency Получает упорядоченный по уровню зависимости список файлов
     * @throws Exception Выбрасывает сообщение об ошибке, если не удается совершить getPath(another.file)
     */
    public void buildFiles(Vector<FileObject> orderedByDependency) throws Exception {
        System.out.println("Building files...");
        for (FileObject file : orderedByDependency) {
            for (FileObject another : orderedByNameFiles) {
                if (file.text.contains("require ‘" + getPath(another.file) + "’")) {
                    file.text = file.text.replaceAll("require ‘" + getPath(another.file) + "’", another.text);
                }
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

