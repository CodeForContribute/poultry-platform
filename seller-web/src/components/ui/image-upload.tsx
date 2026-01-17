"use client";

import * as React from "react";
import { useCallback, useState } from "react";
import { Upload, X, ImageIcon, Loader2, AlertCircle } from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "./button";

const MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
const ACCEPTED_FILE_TYPES = ["image/jpeg", "image/png", "image/webp"];
const ACCEPTED_EXTENSIONS = [".jpg", ".jpeg", ".png", ".webp"];

export interface ImageFile {
  id: string;
  file?: File;
  url: string;
  name: string;
  size?: number;
  isUploading?: boolean;
  uploadProgress?: number;
  error?: string;
}

export interface ImageUploadProps {
  value: ImageFile[];
  onChange: (images: ImageFile[]) => void;
  onUpload?: (file: File) => Promise<{ url: string; id: string }>;
  onDelete?: (imageId: string) => Promise<void>;
  maxImages?: number;
  disabled?: boolean;
  className?: string;
}

function generateId(): string {
  return `img_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`;
}

function validateFile(file: File): string | null {
  if (!ACCEPTED_FILE_TYPES.includes(file.type)) {
    return `Invalid file type. Accepted types: ${ACCEPTED_EXTENSIONS.join(", ")}`;
  }
  if (file.size > MAX_FILE_SIZE) {
    return `File size exceeds 5MB limit. Current size: ${(file.size / 1024 / 1024).toFixed(2)}MB`;
  }
  return null;
}

export function ImageUpload({
  value,
  onChange,
  onUpload,
  onDelete,
  maxImages = 10,
  disabled = false,
  className,
}: ImageUploadProps) {
  const [isDragging, setIsDragging] = useState(false);
  const [dragError, setDragError] = useState<string | null>(null);
  const fileInputRef = React.useRef<HTMLInputElement>(null);

  const canAddMore = value.length < maxImages;

  const handleFiles = useCallback(
    async (files: FileList | null) => {
      if (!files || files.length === 0) return;
      setDragError(null);

      const remainingSlots = maxImages - value.length;
      if (remainingSlots <= 0) {
        setDragError(`Maximum ${maxImages} images allowed`);
        return;
      }

      const filesToProcess = Array.from(files).slice(0, remainingSlots);
      const newImages: ImageFile[] = [];

      for (const file of filesToProcess) {
        const validationError = validateFile(file);
        if (validationError) {
          setDragError(validationError);
          continue;
        }

        const imageFile: ImageFile = {
          id: generateId(),
          file,
          url: URL.createObjectURL(file),
          name: file.name,
          size: file.size,
          isUploading: !!onUpload,
          uploadProgress: 0,
        };

        newImages.push(imageFile);
      }

      if (newImages.length === 0) return;

      const updatedImages = [...value, ...newImages];
      onChange(updatedImages);

      if (onUpload) {
        for (const imageFile of newImages) {
          if (!imageFile.file) continue;

          try {
            const result = await onUpload(imageFile.file);
            onChange(
              updatedImages.map((img) =>
                img.id === imageFile.id
                  ? {
                      ...img,
                      id: result.id,
                      url: result.url,
                      isUploading: false,
                      uploadProgress: 100,
                    }
                  : img
              )
            );
          } catch (error) {
            onChange(
              updatedImages.map((img) =>
                img.id === imageFile.id
                  ? {
                      ...img,
                      isUploading: false,
                      error:
                        error instanceof Error
                          ? error.message
                          : "Upload failed",
                    }
                  : img
              )
            );
          }
        }
      }
    },
    [value, onChange, onUpload, maxImages]
  );

  const handleDragEnter = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(true);
  }, []);

  const handleDragLeave = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
  }, []);

  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
  }, []);

  const handleDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault();
      e.stopPropagation();
      setIsDragging(false);

      if (disabled) return;

      const { files } = e.dataTransfer;
      handleFiles(files);
    },
    [disabled, handleFiles]
  );

  const handleFileInputChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      handleFiles(e.target.files);
      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
    },
    [handleFiles]
  );

  const handleRemoveImage = useCallback(
    async (imageToRemove: ImageFile) => {
      if (onDelete && !imageToRemove.file) {
        try {
          await onDelete(imageToRemove.id);
        } catch (error) {
          console.error("Failed to delete image:", error);
          return;
        }
      }

      if (imageToRemove.file && imageToRemove.url.startsWith("blob:")) {
        URL.revokeObjectURL(imageToRemove.url);
      }

      onChange(value.filter((img) => img.id !== imageToRemove.id));
    },
    [value, onChange, onDelete]
  );

  const openFilePicker = useCallback(() => {
    fileInputRef.current?.click();
  }, []);

  return (
    <div className={cn("space-y-4", className)}>
      {canAddMore && (
        <div
          onDragEnter={handleDragEnter}
          onDragLeave={handleDragLeave}
          onDragOver={handleDragOver}
          onDrop={handleDrop}
          onClick={disabled ? undefined : openFilePicker}
          className={cn(
            "relative flex flex-col items-center justify-center gap-2 rounded-lg border-2 border-dashed p-6 transition-colors",
            isDragging
              ? "border-primary bg-primary/5"
              : "border-muted-foreground/25 hover:border-muted-foreground/50",
            disabled
              ? "cursor-not-allowed opacity-50"
              : "cursor-pointer hover:bg-accent/50",
            dragError && "border-destructive"
          )}
        >
          <input
            ref={fileInputRef}
            type="file"
            accept={ACCEPTED_FILE_TYPES.join(",")}
            multiple
            onChange={handleFileInputChange}
            disabled={disabled}
            className="hidden"
          />

          <div
            className={cn(
              "flex h-12 w-12 items-center justify-center rounded-full bg-muted",
              isDragging && "bg-primary/10"
            )}
          >
            <Upload
              className={cn(
                "h-6 w-6 text-muted-foreground",
                isDragging && "text-primary"
              )}
            />
          </div>

          <div className="text-center">
            <p className="text-sm font-medium">
              {isDragging ? "Drop images here" : "Drag & drop images here"}
            </p>
            <p className="text-xs text-muted-foreground">
              or click to browse files
            </p>
          </div>

          <p className="text-xs text-muted-foreground">
            JPG, PNG, WebP up to 5MB each. Max {maxImages} images.
          </p>

          {dragError && (
            <div className="flex items-center gap-1 text-xs text-destructive">
              <AlertCircle className="h-3 w-3" />
              {dragError}
            </div>
          )}
        </div>
      )}

      {value.length > 0 && (
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4">
          {value.map((image) => (
            <div
              key={image.id}
              className="group relative aspect-square overflow-hidden rounded-lg border bg-muted"
            >
              {image.url ? (
                <img
                  src={image.url}
                  alt={image.name}
                  className={cn(
                    "h-full w-full object-cover",
                    image.isUploading && "opacity-50"
                  )}
                />
              ) : (
                <div className="flex h-full w-full items-center justify-center">
                  <ImageIcon className="h-8 w-8 text-muted-foreground" />
                </div>
              )}

              {image.isUploading && (
                <div className="absolute inset-0 flex flex-col items-center justify-center bg-background/80">
                  <Loader2 className="h-6 w-6 animate-spin text-primary" />
                  <span className="mt-1 text-xs text-muted-foreground">
                    Uploading...
                  </span>
                </div>
              )}

              {image.error && (
                <div className="absolute inset-0 flex flex-col items-center justify-center bg-destructive/20 p-2">
                  <AlertCircle className="h-6 w-6 text-destructive" />
                  <span className="mt-1 text-center text-xs text-destructive">
                    {image.error}
                  </span>
                </div>
              )}

              {!disabled && !image.isUploading && (
                <Button
                  type="button"
                  variant="destructive"
                  size="icon"
                  className="absolute right-1 top-1 h-6 w-6 opacity-0 transition-opacity group-hover:opacity-100"
                  onClick={(e) => {
                    e.stopPropagation();
                    handleRemoveImage(image);
                  }}
                >
                  <X className="h-3 w-3" />
                </Button>
              )}

              <div className="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-black/60 to-transparent p-2 opacity-0 transition-opacity group-hover:opacity-100">
                <p className="truncate text-xs text-white">{image.name}</p>
                {image.size && (
                  <p className="text-xs text-white/70">
                    {(image.size / 1024).toFixed(1)} KB
                  </p>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {value.length > 0 && (
        <p className="text-xs text-muted-foreground">
          {value.length} of {maxImages} images
        </p>
      )}
    </div>
  );
}

export { MAX_FILE_SIZE, ACCEPTED_FILE_TYPES, ACCEPTED_EXTENSIONS };
