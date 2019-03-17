package file_creator;

import file_creator.user.DateOfBirth;
import file_creator.user.Location;
import file_creator.user.User;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;

public class PeopleTableFilesCreator {
    private static String excelFilePath = new File("PeopleTable.xlsx").getAbsolutePath();

    private static void addExcelTableTitleRow(XSSFSheet sheet) {
        XSSFRow titleRow = sheet.createRow(0);
        int currentColumnIndex = 0;
        for (TableFields tableFields : TableFields.values()) {
            XSSFCell nextTitleCell = titleRow.createCell(currentColumnIndex);
            nextTitleCell.setCellValue(tableFields.getField());
            currentColumnIndex++;
        }
    }

    private static void addUserToExcelTableRow(User user, XSSFSheet sheet, int rowNum) {
        XSSFRow userRow = sheet.createRow(rowNum);
        ArrayList<String> personAttributes = user.getStringAttributes();
        for (int fieldNum = 0; fieldNum < personAttributes.size(); fieldNum++) {
            XSSFCell nextCell = userRow.createCell(fieldNum);
            nextCell.setCellValue(personAttributes.get(fieldNum));
        }
    }

    public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException {
        XSSFWorkbook wb = new XSSFWorkbook();
        FileOutputStream fos = new FileOutputStream("PeopleTable.xlsx");
        XSSFSheet sheet = wb.createSheet("People");
        sheet.setDefaultColumnWidth(15);
        addExcelTableTitleRow(sheet);

        int numberOfRows = RandomDataGenerator.getNumberOfRows();

        DBOperator dbOperator = new DBOperator();
        Connection conn = dbOperator.getConnectionToDB();

        for (int row = 1; row <= numberOfRows; row++) {
            User user;
            try {
                String jsonStringObject = OnlineFlowOperator.getUserDataJsonString();
                user = OnlineFlowOperator.getUserDataFromWebApi(jsonStringObject);
                user.setPatronymicByGender(user.getGender());
                user.setOneSignGender();
                user.dob.setDate(DateOfBirth.getDateInDDMMYYYY(DateOfBirth.getDateInYYYYMMDD(user.dob.getDate())));
                user.setNat(Location.getFullCountryName(user.getNat()));
                addUserToExcelTableRow(user, sheet, row);

                int userId = dbOperator.getUserIdInDB(conn, user);
                if (userId == 0)
                    dbOperator.insertUserIntoDB(conn, user);
                else
                    dbOperator.updateUserInfo(conn, user);

            } catch (Exception e) {
                int numberOfUsersInDb = dbOperator.getNumberOfUsersInDB(conn);
                if (numberOfUsersInDb >= numberOfRows) {
                    user = dbOperator.getRandomUserFromDB(conn);
                    addUserToExcelTableRow(user, sheet, row);
                }
                else {
                    user = new User().getRandomUserOffline();
                    addUserToExcelTableRow(user, sheet, row);
                    System.out.println("Не удалось получить пользователя ни от randomuser.me, ни из БД\n" +
                            "Пользователь в строке " + (row + 1) + " сгенерен с использованием локальных ресурсов");
                }
            }
        }

        conn.close();

        wb.write(fos);
        fos.close();

        System.out.println("Файл создан. Путь: " + excelFilePath);
    }
}