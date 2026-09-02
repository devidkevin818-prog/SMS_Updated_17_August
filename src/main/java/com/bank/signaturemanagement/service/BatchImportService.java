package com.bank.signaturemanagement.service;

import com.bank.signaturemanagement.entity.*;
import com.bank.signaturemanagement.repository.*;
import tools.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.*;

@Service
public class BatchImportService {
    public record BatchRow(String employeeId,String name,String designation,String department,String branch,
                           String status,String classification,String joiningDate) {}
    public record BatchItemView(ImportBatchItem item,BatchRow row) {}
    private final ImportBatchRepository batches; private final ImportBatchItemRepository items;
    private final EmployeeRepository employees; private final UserRepository users; private final DesignationRepository designations;
    private final DepartmentRepository departments; private final BranchRepository branches; private final EmployeeStatusRepository statuses;
    private final FileStorageService files; private final ObjectMapper json; private final AuditService audit;
    private final AccessControlService access; private final EmployeeVersionService versions; private final EmployeeNumberPolicyService employeeNumberPolicy;

    public BatchImportService(ImportBatchRepository batches,ImportBatchItemRepository items,EmployeeRepository employees,
      UserRepository users,DesignationRepository designations,DepartmentRepository departments,BranchRepository branches,
      EmployeeStatusRepository statuses,FileStorageService files,ObjectMapper json,AuditService audit,
      AccessControlService access,EmployeeVersionService versions,EmployeeNumberPolicyService employeeNumberPolicy){this.batches=batches;this.items=items;this.employees=employees;this.users=users;this.designations=designations;this.departments=departments;this.branches=branches;this.statuses=statuses;this.files=files;this.json=json;this.audit=audit;this.access=access;this.versions=versions;this.employeeNumberPolicy=employeeNumberPolicy;}

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ImportBatch upload(MultipartFile file,Long retryOfId,String username){
        access.require(username,"BATCH_UPLOAD");
        String number="BATCH-"+Year.now().getValue()+"-"+UUID.randomUUID().toString().substring(0,8).toUpperCase(Locale.ROOT);
        ImportBatch batch=new ImportBatch();batch.setBatchNumber(number);batch.setUploadedBy(users.findByUsername(username).orElseThrow());
        batch.setOriginalFilename(file==null||file.getOriginalFilename()==null?"batch":file.getOriginalFilename());batch.setStatus("VALIDATING");
        if(retryOfId!=null)batch.setRetryOf(batches.findById(retryOfId).orElseThrow(()->new IllegalArgumentException("Original batch not found")));
        batches.saveAndFlush(batch);
        List<BatchRow> rows;
        try { batch.setOriginalFilePath(files.storeImportFile(file,number));rows=parse(file); }
        catch (RuntimeException exception) {
            ImportBatchItem item=new ImportBatchItem();item.setBatch(batch);item.setRowNumber(1);item.setRowData("{}");
            item.setRowData(write(new BatchRow("","","","","","","","")));
            item.setStatus("INVALID");item.setErrorDetail(limit(exception.getMessage(),1000));items.save(item);
            batch.setTotalRows(1);batch.setFailedRows(1);batch.setStatus("DRAFT_REVIEW");batches.save(batch);
            audit.record(username,"BATCH_UPLOAD","IMPORT_BATCH",String.valueOf(batch.getId()),null,"SUCCESS",null,batch.getStatus(),exception.getMessage());
            return batch;
        }
        Set<String> seen=new HashSet<>();int invalid=0;int rowNo=1;
        for(BatchRow row:rows){ImportBatchItem item=new ImportBatchItem();item.setBatch(batch);item.setRowNumber(rowNo++);String error=validate(row,seen);item.setRowData(write(row));item.setStatus(error==null?"VALID":"INVALID");item.setErrorDetail(error);items.save(item);if(error!=null)invalid++;}
        if(rows.isEmpty()){ImportBatchItem item=new ImportBatchItem();item.setBatch(batch);item.setRowNumber(1);item.setRowData("{}");item.setStatus("INVALID");item.setErrorDetail("The file contains no employee data rows");items.save(item);invalid=1;}
        batch.setTotalRows(rows.size());batch.setFailedRows(invalid);batch.setSucceededRows(0);batch.setStatus("DRAFT_REVIEW");
        batches.save(batch);audit.record(username,"BATCH_UPLOAD","IMPORT_BATCH",String.valueOf(batch.getId()),null,"SUCCESS",null,batch.getStatus(),"total="+rows.size()+", invalid="+invalid);return batch;
    }

    @Transactional(readOnly=true) public List<ImportBatch> all(){return batches.findByActiveTrueOrderByUploadedAtDesc();}
    @Transactional(readOnly=true) public ImportBatch get(Long id){return batches.findById(id).orElseThrow(()->new IllegalArgumentException("Batch not found"));}
    @Transactional(readOnly=true) public List<ImportBatchItem> items(Long id){return items.findByBatchIdOrderByRowNumber(id);}
    @Transactional(readOnly=true) public List<BatchItemView> itemViews(Long id){get(id);return items.findByBatchIdOrderByRowNumber(id).stream().map(i->new BatchItemView(i,read(i.getRowData()))).toList();}
    @Transactional(readOnly=true) public List<ImportBatch> pending(String level){return batches.findByStatusOrderByUploadedAtAsc("DGM".equals(level)?"PENDING_DGM":"PENDING_GM");}

    @Transactional
    public void updateRow(Long batchId,Long itemId,BatchRow row,String username){
        access.require(username,"BATCH_UPLOAD");ImportBatch batch=editableBatch(batchId);
        ImportBatchItem item=items.findByIdAndBatchId(itemId,batchId).orElseThrow(()->new IllegalArgumentException("Batch row not found"));
        String old=item.getRowData();item.setRowData(write(row));revalidate(batch);
        audit.record(username,"BATCH_ROW_UPDATE","IMPORT_BATCH_ITEM",itemId.toString(),null,"SUCCESS",old,item.getRowData(),batch.getBatchNumber());
    }

    @Transactional
    public void submit(Long batchId,String username){
        access.require(username,"BATCH_UPLOAD");ImportBatch batch=editableBatch(batchId);revalidate(batch);
        if(batch.getTotalRows()==0)throw new IllegalArgumentException("Add at least one employee row before submitting");
        if(batch.getFailedRows()>0)throw new IllegalArgumentException("Correct every invalid row before submitting the batch");
        batch.setStatus("PENDING_DGM");
        audit.record(username,"BATCH_SUBMIT","IMPORT_BATCH",batchId.toString(),null,"SUCCESS","DRAFT_REVIEW","PENDING_DGM",null);
    }

    @Transactional
    public void cancel(Long batchId,String username){access.require(username,"BATCH_UPLOAD");ImportBatch batch=editableBatch(batchId);batch.setStatus("CANCELLED");batch.setActive(false);audit.record(username,"BATCH_CANCEL","IMPORT_BATCH",batchId.toString(),null,"SUCCESS","active","inactive",null);}

    @Transactional
    public void decide(Long id,String level,String action,String comment,String username){
        level=clean(level).toUpperCase(Locale.ROOT);action=clean(action).toUpperCase(Locale.ROOT);
        if(!Set.of("DGM","GM").contains(level))throw new IllegalArgumentException("Invalid approval level");
        if(!Set.of("APPROVE","REJECT").contains(action))throw new IllegalArgumentException("Invalid approval action");
        access.require(username,"DGM".equals(level)?"APPROVE_DGM":"APPROVE_GM");
        User actor=users.findByUsername(username).orElseThrow();
        if(!level.equals(actor.getRole().getName())&&!"ADMIN".equals(actor.getRole().getName()))throw new org.springframework.security.access.AccessDeniedException("Wrong approval level");
        ImportBatch batch=get(id);String expected="PENDING_"+level;if(!expected.equals(batch.getStatus()))throw new IllegalArgumentException("Batch is not awaiting "+level);
        LocalDateTime now=LocalDateTime.now();
        if("DGM".equals(level)){batch.setDgmDecidedBy(actor);batch.setDgmDecidedAt(now);batch.setDgmComment(clean(comment));}
        else{batch.setGmDecidedBy(actor);batch.setGmDecidedAt(now);batch.setGmComment(clean(comment));}
        if("REJECT".equals(action)){if(comment==null||comment.isBlank())throw new IllegalArgumentException("Rejection reason is required");batch.setStatus("REJECTED");batch.setRejectionReason(comment.trim());}
        else if("DGM".equals(level)){batch.setStatus("PENDING_GM");}
        else{effectuate(batch,username);}
        audit.record(username,"BATCH_"+level+"_"+action,"IMPORT_BATCH",id.toString(),null,"SUCCESS",expected,batch.getStatus(),comment);
    }

    private void effectuate(ImportBatch batch,String actor){int success=0;int failed=batch.getFailedRows();for(ImportBatchItem item:items.findByBatchIdAndStatusOrderByRowNumber(batch.getId(),"VALID")){try{BatchRow row=json.readValue(item.getRowData(),BatchRow.class);String code=employeeNumberPolicy.normalize(row.employeeId());if(employees.existsByEmployeeNumber(code))throw new IllegalArgumentException("Employee ID became a duplicate");Employee e=new Employee();e.setEmployeeNumber(code);e.setFullName(row.name().trim());e.setDesignation(findDesignation(row.designation()));e.setDepartment(findDepartment(row.department()));e.setBranch(findBranch(row.branch()));e.setEmployeeStatus(findStatus(row.status()));e.setClassification(scope(row.classification()));e.setJoiningDate(LocalDate.parse(row.joiningDate()));e.setBatchId(batch.getId());e.setActive(true);employees.saveAndFlush(e);versions.append(e,actor,"Batch "+batch.getBatchNumber());item.setEmployee(e);item.setStatus("CREATED");success++;audit.record(actor,"BATCH_EMPLOYEE_CREATE","EMPLOYEE",String.valueOf(e.getId()),null,"SUCCESS",null,code,batch.getBatchNumber());}catch(Exception ex){item.setStatus("FAILED");item.setErrorDetail(limit(ex.getMessage(),1000));failed++;}}
        batch.setSucceededRows(success);batch.setFailedRows(failed);batch.setStatus(failed==0?"COMPLETED":"COMPLETED_WITH_ERRORS");
    }

    private String validate(BatchRow row,Set<String> seen){List<String> errors=new ArrayList<>();String code=null;try{code=employeeNumberPolicy.normalize(row.employeeId());}catch(Exception e){errors.add("invalid employee_id: "+e.getMessage());}if(code!=null&&(!seen.add(code)||employees.existsByEmployeeNumber(code)))errors.add("duplicate employee_id");if(blank(row.name()))errors.add("name is required");try{findDesignation(row.designation());}catch(Exception e){errors.add(e.getMessage());}try{findDepartment(row.department());}catch(Exception e){errors.add(e.getMessage());}try{findBranch(row.branch());}catch(Exception e){errors.add(e.getMessage());}try{findStatus(row.status());}catch(Exception e){errors.add(e.getMessage());}try{scope(row.classification());}catch(Exception e){errors.add(e.getMessage());}try{LocalDate.parse(row.joiningDate());}catch(Exception e){errors.add("joining_date must be YYYY-MM-DD");}return errors.isEmpty()?null:String.join("; ",errors);}
    private ImportBatch editableBatch(Long id){ImportBatch b=get(id);if(!Set.of("DRAFT_REVIEW","VALIDATION_FAILED","REJECTED").contains(b.getStatus()))throw new IllegalStateException("This batch can no longer be edited");return b;}
    private void revalidate(ImportBatch batch){Set<String> seen=new HashSet<>();int invalid=0;List<ImportBatchItem> batchItems=items.findByBatchIdOrderByRowNumber(batch.getId());for(ImportBatchItem item:batchItems){String error=validate(read(item.getRowData()),seen);item.setStatus(error==null?"VALID":"INVALID");item.setErrorDetail(error);if(error!=null)invalid++;}batch.setTotalRows(batchItems.size());batch.setFailedRows(invalid);batch.setStatus("DRAFT_REVIEW");}
    private BatchRow read(String value){try{return json.readValue(value,BatchRow.class);}catch(Exception e){return new BatchRow("","","","","","","","");}}
    private List<BatchRow> parse(MultipartFile file){String name=file.getOriginalFilename()==null?"":file.getOriginalFilename().toLowerCase(Locale.ROOT);try{return name.endsWith(".xlsx")?parseExcel(file):parseCsv(file);}catch(IOException e){throw new IllegalArgumentException("Could not parse batch file: "+e.getMessage(),e);}}
    private List<BatchRow> parseCsv(MultipartFile file)throws IOException{List<BatchRow> result=new ArrayList<>();try(var reader=new InputStreamReader(file.getInputStream(),StandardCharsets.UTF_8);var parser=CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setIgnoreHeaderCase(true).setTrim(true).get().parse(reader)){validateHeaders(parser.getHeaderMap().keySet());for(var r:parser)result.add(new BatchRow(value(r,"employee_id"),value(r,"name"),value(r,"designation"),value(r,"department"),value(r,"branch"),value(r,"status"),value(r,"classification"),value(r,"joining_date")));}return result;}
    private List<BatchRow> parseExcel(MultipartFile file)throws IOException{List<BatchRow> result=new ArrayList<>();try(Workbook workbook=WorkbookFactory.create(file.getInputStream())){if(workbook.getNumberOfSheets()<1)throw new IllegalArgumentException("Workbook contains no worksheet");Sheet sheet=workbook.getSheetAt(0);if(sheet.getPhysicalNumberOfRows()<1)return result;DataFormatter f=new DataFormatter();Map<String,Integer> h=new HashMap<>();Row header=sheet.getRow(sheet.getFirstRowNum());if(header==null)throw new IllegalArgumentException("Header row is missing");for(Cell c:header)h.put(f.formatCellValue(c).trim().toLowerCase(Locale.ROOT),c.getColumnIndex());validateHeaders(h.keySet());for(int i=sheet.getFirstRowNum()+1;i<=sheet.getLastRowNum();i++){Row r=sheet.getRow(i);if(r==null)continue;result.add(new BatchRow(cell(r,h,"employee_id",f),cell(r,h,"name",f),cell(r,h,"designation",f),cell(r,h,"department",f),cell(r,h,"branch",f),cell(r,h,"status",f),cell(r,h,"classification",f),cell(r,h,"joining_date",f)));}}return result;}
    private void validateHeaders(Collection<String> supplied){Set<String> normalized=new HashSet<>();for(String h:supplied)normalized.add(h.trim().toLowerCase(Locale.ROOT));Set<String> required=Set.of("employee_id","name","designation","department","branch","status","classification","joining_date");Set<String> missing=new TreeSet<>(required);missing.removeAll(normalized);if(!missing.isEmpty())throw new IllegalArgumentException("Missing required columns: "+String.join(", ",missing));}
    private String value(org.apache.commons.csv.CSVRecord r,String key){return r.isMapped(key)?r.get(key):"";} private String cell(Row r,Map<String,Integer> h,String key,DataFormatter f){Integer i=h.get(key);return i==null?"":f.formatCellValue(r.getCell(i)).trim();}
    private Designation findDesignation(String n){return designations.findAll().stream().filter(v->v.getDesignationName().equalsIgnoreCase(clean(n))&&Boolean.TRUE.equals(v.getIsActive())).findFirst().orElseThrow(()->new IllegalArgumentException("unknown/inactive designation"));}
    private Department findDepartment(String n){return departments.findAll().stream().filter(v->v.getDepartmentName().equalsIgnoreCase(clean(n))&&Boolean.TRUE.equals(v.getActive())).findFirst().orElseThrow(()->new IllegalArgumentException("unknown/inactive department"));}
    private Branch findBranch(String n){return branches.findAll().stream().filter(v->v.getBranchName().equalsIgnoreCase(clean(n))&&v.isActive()).findFirst().orElseThrow(()->new IllegalArgumentException("unknown/inactive branch"));}
    private EmployeeStatus findStatus(String n){return statuses.findAll().stream().filter(v->v.getStatusName().equalsIgnoreCase(clean(n))&&v.isActive()).findFirst().orElseThrow(()->new IllegalArgumentException("unknown/inactive status"));}
    private String scope(String v){String s=clean(v).toUpperCase(Locale.ROOT);if(!Set.of("LOCAL","FOREIGN","BOTH").contains(s))throw new IllegalArgumentException("classification must be LOCAL, FOREIGN, or BOTH");return s;}
    private String write(BatchRow row){try{return json.writeValueAsString(row);}catch(Exception e){throw new IllegalStateException(e);}} private boolean blank(String v){return v==null||v.isBlank();} private String clean(String v){return v==null?"":v.trim();} private String limit(String v,int n){return v==null?null:v.substring(0,Math.min(v.length(),n));}
}
