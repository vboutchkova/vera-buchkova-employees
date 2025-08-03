import { Component, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common'; 
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-upload',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './upload.html',
  styleUrls: ['./upload.scss']
})
export class UploadComponent {
  @ViewChild('fileInput') inputRef!: ElementRef<HTMLInputElement>;
  selectedFile: File | null = null;
  message = '';

  resultRows: { emp1: string; emp2: string; projectId: string; days: number }[] = [];

  constructor(private http: HttpClient) {}


  onFileSelected(event: any): void {
    this.clear();
    this.selectedFile = event.target.files[0];
  }

  onUpload() {
    if (!this.selectedFile) {
      this.message = 'Select a file!';
      return;
    }

    const formData = new FormData();
    formData.append('file', this.selectedFile);

    this.http.post<any>('http://localhost:8080/employee-pairs/analyze', formData)
      .subscribe({
        next: (response) => {
          this.message = response.maxPairResultMessage;
          const emp1 = response.employee1;
          const emp2 = response.employee2;
          const allProjects = response.allCommonProjects;

          this.resultRows = Object.entries(allProjects).map(([projectId, days]) => ({
            emp1,
            emp2,
            projectId,
            days: Number(days)
          }));
        },
        error: (err) => {
          console.error(err);
          this.message = 'Грешка при качване на файла.';
        }
      });
  }

  clear() {
    this.message = '';
    this.resultRows = [];
  }

  clearAll() {
    this.clear();
    this.selectedFile = null;
    this.inputRef.nativeElement.value = '';
  }
}
