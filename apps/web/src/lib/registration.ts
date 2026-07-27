export interface RegistrationValues {
  displayName: string;
  email: string;
  password: string;
}

export function validateRegistration(values: RegistrationValues): Record<string, string> {
  const errors: Record<string, string> = {};
  if (!values.displayName.trim()) errors.displayName = "Enter your name.";
  if (!values.email.trim()) errors.email = "Enter your email address.";
  else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(values.email.trim()))
    errors.email = "Enter a valid email address.";
  if (values.password.length < 12) errors.password = "Use at least 12 characters.";
  if (values.password.length > 128) errors.password = "Use no more than 128 characters.";
  return errors;
}
