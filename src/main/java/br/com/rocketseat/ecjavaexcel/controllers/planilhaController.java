package br.com.rocketseat.ecjavaexcel.controllers;

import br.com.rocketseat.ecjavaexcel.models.User;
import br.com.rocketseat.ecjavaexcel.repositories.UsersRepository;
import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
public class planilhaController {

  @Autowired
  UsersRepository usersRepository;

  @PostMapping("/")
  public List<User> upload(@RequestParam("file") MultipartFile file)
    throws IOException {
    Workbook nossoWorkbook = new XSSFWorkbookFactory()
    .create(file.getInputStream());

    Sheet nossaAba = nossoWorkbook.getSheetAt(0);

    List<User> listaUsers = new ArrayList<>();

    for (Row row : nossaAba) {
      if (row.getRowNum() == 0) {
        continue;
      }

      String nome = row.getCell(0).getStringCellValue();
      Integer idade = (int) row.getCell(1).getNumericCellValue();
      LocalDate data_nascimento = row
        .getCell(2)
        .getLocalDateTimeCellValue()
        .toLocalDate();
      BigDecimal saldo = BigDecimal.valueOf(
        row.getCell(3).getNumericCellValue()
      );

      //Tratativa para nome maiusculo
      nome = nome.toUpperCase();

      User novoUser = new User();

      novoUser.setNome(nome);
      novoUser.setIdade(idade);
      novoUser.setData_nascimento(data_nascimento);
      novoUser.setSaldo(saldo);

      listaUsers.add(novoUser);
    }

    return usersRepository.saveAll(listaUsers);
  }

  @ResponseBody
  @GetMapping("/")
  public ResponseEntity<ByteArrayResource> export() throws IOException {
    List<User> usersList = usersRepository.findAll();

    Workbook nossoExcel = new XSSFWorkbook();
    String nomeDaAba = WorkbookUtil.createSafeSheetName("[Relatório]");
    Sheet nossaAba = nossoExcel.createSheet(nomeDaAba);

    CellStyle headerStyle = nossoExcel.createCellStyle();
    headerStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
    headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

    headerStyle.setBorderBottom(BorderStyle.THIN);
    headerStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());

    headerStyle.setBorderLeft(BorderStyle.THIN);
    headerStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex());

    headerStyle.setBorderRight(BorderStyle.THIN);
    headerStyle.setRightBorderColor(IndexedColors.BLACK.getIndex());

    headerStyle.setBorderTop(BorderStyle.THIN);
    headerStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());

    var headerFields = Arrays.asList(
      "Nome",
      "Idade",
      "Data nascimento",
      "Saldo",
      "Taxa",
      "Saldo final"
    );

    Row rowHeader = nossaAba.createRow(0);

    headerFields.forEach(
      field -> {
        Cell headerCell = rowHeader.createCell(headerFields.indexOf(field));
        headerCell.setCellValue(field);
        headerCell.setCellStyle(headerStyle);
      }
    );

    usersList.forEach(
      user -> {
        var currentUser = user;
        int indice = usersList.indexOf(user) + 1;
        Row userRow = nossaAba.createRow(indice);

        Cell colunaNome = userRow.createCell(0);
        colunaNome.setCellValue(currentUser.getNome());

        Cell colunaIdade = userRow.createCell(1);
        colunaIdade.setCellValue(currentUser.getIdade());

        Cell colunaDataNascimento = userRow.createCell(2);
        colunaDataNascimento.setCellValue(currentUser.getData_nascimento());

        Cell colunaSaldo = userRow.createCell(3);
        String colunaSaldoLetra = CellReference.convertNumToColString(3);
        colunaSaldo.setCellValue(currentUser.getSaldo().doubleValue());

        Cell colunaTaxa = userRow.createCell(4);
        String colunaTaxaLetra = CellReference.convertNumToColString(4);
        colunaTaxa.setCellValue(250.00);

        Integer linhaAtual = indice + 1;

        Cell ColunaSaldoTotal = userRow.createCell(5);
        ColunaSaldoTotal.setCellFormula(
          "(" +
          colunaSaldoLetra +
          linhaAtual +
          "-" +
          colunaTaxaLetra +
          linhaAtual +
          ")"
        );
      }
    );

    HttpHeaders responseHeaders = new HttpHeaders();

    responseHeaders.setContentType(
      new MediaType("application", "force-download")
    );
    responseHeaders.set(
      HttpHeaders.CONTENT_DISPOSITION,
      "attachment; filename=relatorio_usuarios.xlsx"
    );

    ByteArrayOutputStream stream = new ByteArrayOutputStream();

    nossoExcel.write(stream);
    nossoExcel.close();

    return new ResponseEntity<ByteArrayResource>(
      new ByteArrayResource(stream.toByteArray()),
      responseHeaders,
      HttpStatus.OK
    );
  }
}
