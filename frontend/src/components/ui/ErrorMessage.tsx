export function ErrorMessage({ message }: { message: string }) {
  return (
    <div className="rounded-md bg-destructive/10 border border-destructive/20 p-4 text-destructive text-sm">
      {message}
    </div>
  );
}
